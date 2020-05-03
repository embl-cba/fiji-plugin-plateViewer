package de.embl.cba.plateviewer.table;

import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import de.embl.cba.plateviewer.image.MultiWellChannelFilesProviderBatchLibHdf5;
import de.embl.cba.plateviewer.image.NamingSchemes;
import de.embl.cba.tables.TableColumns;
import net.imglib2.Interval;

import java.util.*;

public class Tables
{

	public static List< DefaultAnnotatedIntervalTableRow > createSiteNameTableRowsFromColumns(
			final Map< String, List< String > > columns,
			final String siteNameColumnName,
			Map< String, Interval > siteNameToInterval,
			final String outlierColumnName )
	{
		final List< DefaultAnnotatedIntervalTableRow > siteNameTableRows = new ArrayList<>();

		final int numRows = columns.values().iterator().next().size();

		for ( int row = 0; row < numRows; row++ )
		{
			final String siteName = columns.get( siteNameColumnName ).get( row );

			Interval interval = null;
			if ( siteNameToInterval != null )
				interval = siteNameToInterval.get( siteName );

			siteNameTableRows.add(
					new DefaultAnnotatedIntervalTableRow(
							siteName,
							interval,
							outlierColumnName,
							columns,
							row )
			);
		}

		return siteNameTableRows;
	}

	public static List< DefaultAnnotatedIntervalTableRow > createSiteTableRowsFromFile(
			String filePath,
			String imageNamingScheme,
			Map< String, Interval > siteNameToInterval )
	{
		final Map< String, List< String > > columnNameToColumn;

		if ( filePath.endsWith( ".csv" ) )
		{
			columnNameToColumn = TableColumns.stringColumnsFromTableFile( filePath );
		}
		else if ( filePath.endsWith( ".hdf5" ) || filePath.endsWith( ".h5" ) )
		{
			columnNameToColumn = Tables.stringColumnsFromHDF5( filePath, "tables/images/default" );
		}
		else
		{
			throw new UnsupportedOperationException( "Table file extension not supported: " + filePath );
		}

		if ( imageNamingScheme.equals( NamingSchemes.PATTERN_NIKON_TI2_HDF5 ) )
		{
			final String siteNameColumnName = ensureSiteNameColumn( columnNameToColumn );

			return createSiteNameTableRowsFromColumns(
						columnNameToColumn,
						siteNameColumnName,
						siteNameToInterval,
						NamingSchemes.ColumnNamesBatchLibHdf5.COLUMN_NAME_OUTLIER );
		}
		else
		{
			throw new UnsupportedOperationException( "Appending a table for naming scheme " + imageNamingScheme + " is not yet supported.");
		}
	}

	public static String ensureSiteNameColumn( Map< String, List< String > > columnNameToColumn )
	{
		if ( columnNameToColumn.keySet().contains( "site_name" ) )
		{
			return "site_name";
		}
		else if ( columnNameToColumn.keySet().contains( "site-name" ) )
		{
			return "site-name";
		}
		else
		{
			final int numRows = columnNameToColumn.values().iterator().next().size();

			final List< String > siteNameColumn = new ArrayList<>();

			for ( int rowIndex = 0; rowIndex < numRows; rowIndex++ )
			{
				final String imageFileName = columnNameToColumn.get( "image" ).get( rowIndex ) + ".h5";
				final String siteName = MultiWellChannelFilesProviderBatchLibHdf5.createSiteName( imageFileName );
				siteNameColumn.add( siteName );
			}

			columnNameToColumn.put( "site_name", siteNameColumn );

			return "site_name";
		}
	}

	public static Map< String, List< String > > stringColumnsFromHDF5( final String filePath, String tableGroup )
	{
		final IHDF5Reader hdf5Reader = HDF5Factory.openForReading( filePath );
		final List< String > groupMembers = hdf5Reader.getGroupMembers( "/" );

		final byte[] bytes = hdf5Reader.uint8().readArray( tableGroup + "/visible" );

		final String[] columnNames = hdf5Reader.string().readMDArray( tableGroup + "/columns" ).getAsFlatArray();
		final String[] cells = hdf5Reader.string().readMDArray( tableGroup + "/cells" ).getAsFlatArray();

		final Map< String, List< String > > columnNameToStrings = new LinkedHashMap<>();

		final int numColumns = columnNames.length;

		for ( int columnIndex = 0; columnIndex < numColumns; columnIndex++ )
		{
			final String columnName = columnNames[ columnIndex ];
			columnNameToStrings.put( columnName, new ArrayList<>( ) );
		}

		final int numRows = cells.length / columnNames.length;

		int cellIndex = 0;
		for ( int rowIndex = 0; rowIndex < numRows; ++rowIndex )
		{
			for ( int columnIndex = 0; columnIndex < numColumns; columnIndex++ )
			{
				columnNameToStrings
						.get( columnNames[ columnIndex ] )
						.add( cells[ cellIndex++ ] );
			}
		}

		// System.out.println( ( System.currentTimeMillis() - start ) / 1000.0 ) ;

		return columnNameToStrings;
	}

}