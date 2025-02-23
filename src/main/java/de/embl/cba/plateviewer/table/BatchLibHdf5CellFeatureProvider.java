package de.embl.cba.plateviewer.table;

import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import com.google.common.util.concurrent.UncheckedTimeoutException;

import java.io.File;
import java.util.*;

public class BatchLibHdf5CellFeatureProvider
{
	private final String plateDirectory;
	private List< File > siteFiles;
	private ArrayList< String > tableGroups;
	private HashMap< String, Map< String, List< String > > > siteAndTableGroupToColumns;

	public BatchLibHdf5CellFeatureProvider( String plateDirectory, List< File > siteFiles )
	{
		this.plateDirectory = plateDirectory;
		this.siteFiles = siteFiles;
		siteAndTableGroupToColumns = new HashMap<>();
		final IHDF5Reader hdf5Reader = HDF5Factory.openForReading( siteFiles.get( 0 ) );
		fetchTableGroups( hdf5Reader );
	}

	public String fetchFeature( int labelId, String siteName, String tableGroup, String feature )
	{
		if ( siteFiles == null )
		{
			throw new UnsupportedOperationException( "Fetching site files not yet implemented" );
		}

		final Map< String, List< String > > columns = getColumns( siteName, tableGroup );

		final List< String > labels = columns.get( "label_id" );
		for ( int i = 0; i < labels.size(); i++ )
		{
			final int label = ( int ) Double.parseDouble( labels.get( i ) );

			if ( label ==  labelId  )
			{
				final String value = columns.get( feature ).get( i );
				return value;
			}
		}

		throw new UnsupportedOperationException( "Feature " + feature + "not found for cell " + labelId );
	}

	private void fetchTableGroups( IHDF5Reader hdf5Reader )
	{
		tableGroups = new ArrayList<>();
		setLeafGroups( hdf5Reader, "/tables" );
	}

	public void setLeafGroups( IHDF5Reader hdf5Reader, String parentGroup )
	{
		final List< String > groupMembers = hdf5Reader.getGroupMembers( parentGroup );
		final List< String > childGroups = new ArrayList<>(  );
		for ( String groupMember : groupMembers )
		{
			if ( hdf5Reader.isGroup( parentGroup + "/" + groupMember ) )
			{
				childGroups.add( parentGroup + "/" + groupMember );
			}
		}

		if ( childGroups.size() == 0 )
		{
			tableGroups.add( parentGroup );
		}
		else
		{
			for ( String childGroup : childGroups )
			{
				setLeafGroups( hdf5Reader, childGroup );
			}
		}
	}

	public File getSiteFile( String siteName )
	{
		final String replace = siteName.replace( "-", "_" );
		for ( File siteFile : siteFiles )
		{
			if ( siteFile.getName().contains( replace ) )
			{
				return siteFile;
			}
		}
		throw new UnsupportedOperationException( "No file not found for site: " + siteName );
	}

	public ArrayList< String > getTableGroups()
	{
		return tableGroups;
	}

	public Set< String > getFeatureNames( String siteName, String tableGroup )
	{
		final Map< String, List< String > > columns = getColumns( siteName, tableGroup );

		return columns.keySet();
	}

	private Map< String, List< String > > getColumns( String siteName, String tableGroup )
	{
		final String key = getKey( siteName, tableGroup );

		if ( ! siteAndTableGroupToColumns.containsKey( key ) )
		{
			final Map< String, List< String > > columns = Tables.stringColumnsFromHDF5(
					getSiteFile( siteName ).getAbsolutePath(),
					tableGroup );

			siteAndTableGroupToColumns.put( key, columns );
		}

		return siteAndTableGroupToColumns.get( key );
	}

	public String getKey( String siteName, String tableGroup )
	{
		return siteName + tableGroup;
	}
}