/*

   Licensed Materials - Property of IBM
   Cloudscape - Package org.apache.derby.impl.sql.compile
   (C) Copyright IBM Corp. 1997, 2004. All Rights Reserved.
   US Government Users Restricted Rights - Use, duplication or
   disclosure restricted by GSA ADP Schedule Contract with IBM Corp.

 */

package	org.apache.derby.impl.sql.compile;

import org.apache.derby.iapi.sql.dictionary.DataDictionary;
import org.apache.derby.iapi.sql.dictionary.SchemaDescriptor;
import org.apache.derby.iapi.sql.conn.LanguageConnectionContext;

import org.apache.derby.iapi.error.StandardException;

import org.apache.derby.iapi.services.sanity.SanityManager;

import org.apache.derby.iapi.reference.Property;
import org.apache.derby.iapi.reference.SQLState;

/**
 * A TableName represents a qualified name, externally represented as a schema name
 * and an object name separated by a dot. This class is mis-named: it is used to
 * represent the names of other object types in addition to tables.
 *
 * @author Jerry Brenner
 */

public class TableName extends QueryTreeNode
{
	/* Both schemaName and tableName can be null, however, if 
	** tableName is null then schemaName must also be null.
	*/
	String	tableName;
	String	schemaName;

	/*
	** These fields are used to track the being and end
	** offset of the token from which the TableName came.
	** These are always set to legitimate values in the
	** parser.  If a tableName has been generated elsewhere,
	** they may not be set.  -1 means unset.
	*/
	private int		tokBeginOffset = -1;
	private int		tokEndOffset = -1;

	/**
	 * Initializer for when you have both the table and schema names.
	 *
	 * @param schemaName	The name of the schema being referenced
	 * @param tableName		The name of the table being referenced	 
	 */

	public void init(Object schemaName, Object tableName)
	{
		this.schemaName = (String) schemaName;
		this.tableName = (String) tableName;
	}

	/**
	 * Initializer for when you have both the table and schema names.
	 *
	 * @param schemaName	The name of the schema being referenced
	 * @param tableName		The name of the table being referenced	 
	 * @param tokBeginOffset begin position of token for the table name 
	 *					identifier from parser.  pass in -1 if unknown
	 * @param tokEndOffset	end position of token for the table name 
	 *					identifier from parser.  pass in -1 if unknown
	 */
	public void init
	(
		Object schemaName, 
		Object tableName, 
		Object	tokBeginOffset,
		Object	tokEndOffset
	)
	{
		this.schemaName = (String) schemaName;
		this.tableName = (String) tableName;
		this.tokBeginOffset = ((Integer) tokBeginOffset).intValue();
		this.tokEndOffset = ((Integer) tokEndOffset).intValue();
	}

	/**
	 * Get the table name (without the schema name).
	 *
	 * @return Table name as a String
	 */

	public String getTableName()
	{
		return tableName;
	}

	/**
	 * Get the schema name.
	 *
	 * @return Schema name as a String
	 */

	public String getSchemaName()
	{
		return schemaName;
	}

	/**
	 * Get the begin offset of the parser token for the table name
	 * Will only be set when the TableName was generated by the
	 * parser.
	 *
	 * @return the begin offset of the token.  -1 means unknown
	 */
	public int getTokenBeginOffset()
	{
		return tokBeginOffset;
	}

	/**
	 * Get the end offset of the parser token for the table name.
	 * Will only be set when the TableName was generated by the
	 * parser.
	 *
	 * @return the end offset of the token.  -1 means unknown
	 */
	public int getTokenEndOffset()
	{
		return tokEndOffset;
	}

	/**
	 * Set the schema name.
	 *
	 * @param schemaName	 Schema name as a String
	 *
	 * @return Nothing.
	 */

	public void setSchemaName(String schemaName)
	{
		this.schemaName = schemaName;
	}

	/**
	 * Get the full table name (with the schema name, if explicitly
	 * specified).
	 *
	 * @return Full table name as a String
	 */

	public String getFullTableName()
	{
		if (schemaName != null)
			return schemaName + "." + tableName;
		else
			return tableName;
	}

	/**
	 * Convert this object to a String.  See comments in QueryTreeNode.java
	 * for how this should be done for tree printing.
	 *
	 * @return	This object as a String
	 */

	public String toString()
	{
		return getFullTableName();
	}

	/**
	 * 2 TableNames are equal if their both their schemaNames and tableNames are
	 * equal, or if this node's full table name is null (which happens when a
	 * SELECT * is expanded).  Also, only check table names if the schema
	 * name(s) are null.
	 *
	 * @param otherTableName	The other TableName.
	 *
	 * @return boolean		Whether or not the 2 TableNames are equal.
	 */
	public boolean equals(TableName otherTableName)
	{
		String fullTableName = getFullTableName();
		if (fullTableName == null)
		{
			return true;
		}
		else if ((schemaName == null) || 
				 (otherTableName.getSchemaName() == null))
		{
			return tableName.equals(otherTableName.getTableName());
		}
		else
		{
			return fullTableName.equals(otherTableName.getFullTableName());
		}
	}

	/**
	 * 2 TableNames are equal if their both their schemaNames and tableNames are
	 * equal, or if this node's full table name is null (which happens when a
	 * SELECT * is expanded).  Also, only check table names if the schema
	 * name(s) are null.
	 *
	 * @param otherSchemaName	The other TableName.
	 * @param otherTableName	The other TableName.
	 *
	 * @return boolean		Whether or not the 2 TableNames are equal.
	 */
	public boolean equals(String otherSchemaName, String otherTableName)
	{
		String fullTableName = getFullTableName();
		if (fullTableName == null)
		{
			return true;
		}
		else if ((schemaName == null) || 
				 (otherSchemaName == null))
		{
			return tableName.equals(otherTableName);
		}
		else
		{
			return fullTableName.equals(otherSchemaName+"."+otherTableName);
		}
	}

	///////////////////////////////////////////////////////////////////////
	//
	//	BIND METHODS
	//
	///////////////////////////////////////////////////////////////////////

	/**
	  *	Bind this TableName. This means filling in the schema name if it
	  *	wasn't specified.
	  *
	  *	@param	dataDictionary	Data dictionary to bind against.
	  *
	  *	@exception StandardException		Thrown on error
	  */
	public void	bind( DataDictionary	dataDictionary )
		                       throws StandardException
	{
        schemaName = getSchemaDescriptor(schemaName).getSchemaName();
	}

	///////////////////////////////////////////////////////////////////////
	//
	//	OBJECT INTERFACE
	//
	///////////////////////////////////////////////////////////////////////

	/**
	  *	Returns a hashcode for this tableName. This allows us to use TableNames
	  *	as keys in hash lists.
	  *
	  *	@return	hashcode for this tablename
	  */
	public	int	hashCode()
	{
		return	getFullTableName().hashCode();
	}

	/**
	  *	Compares two TableNames. Needed for hashing logic to work.
	  *
	  *	@param	other	other tableName
	  */
	public	boolean	equals( Object other )
	{
		if ( !( other instanceof TableName ) ) { return false; }

		TableName		that = (TableName) other;

		return	this.getFullTableName().equals( that.getFullTableName() );
	}

}
