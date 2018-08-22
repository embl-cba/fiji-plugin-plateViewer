package de.embl.cba.multipositionviewer;

import net.imglib2.FinalInterval;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageFileListGeneratorMDSingleSite
{
	final ArrayList< File > files;

	int numSites, numWells;
	int[] siteDimensions;
	int[] wellDimensions;
	int[] maxWellDimensionsInData;
	int[] maxSiteDimensionsInData;
	int[] imageDimensions;

	final ArrayList< ImageFile > list;

	final static String namingScheme = Utils.PATTERN_MD_A01_CHANNEL;

	public ImageFileListGeneratorMDSingleSite( ArrayList< File > files, int[] imageDimensions )
	{
		this.files = files;
		this.list = new ArrayList<>();
		this.imageDimensions = imageDimensions;

		this.maxWellDimensionsInData = new int[ 2 ];
		this.maxSiteDimensionsInData = new int[ 2 ];

		createList();

	}

	public ArrayList< ImageFile > getFileList()
	{
		return list;
	}

	private void createList()
	{

		configWells( files );
		configSites( files );

		for ( File file : files )
		{

			final ImageFile imageFile = new ImageFile();

			imageFile.file = file;
			imageFile.interval = getInterval( file, namingScheme, wellDimensions[ 0 ], siteDimensions[ 0 ] );
			list.add( imageFile );

		}
	}

	private void configWells( ArrayList< File > files )
	{
		int[] maximalWellPositionsInData = getMaximalWellPositionsInData( files );
		wellDimensions = new int[ 2 ];

		// TODO...
		if ( numWells <= 24 )
		{
			wellDimensions[ 0 ] = 6;
			wellDimensions[ 1 ] = 4;
		}
		else if ( numWells <= 96  )
		{
			wellDimensions[ 0 ] = 12;
			wellDimensions[ 1 ] = 8;
		}
		else if ( numWells <= 384  )
		{
			wellDimensions[ 0 ] = 24;
			wellDimensions[ 1 ] = 16;
		}
		else
		{
			Utils.log( "ERROR: Could not figure out the correct number of wells...." );
		}

		Utils.log( "Distinct wells: " +  numWells );
		Utils.log( "Well dimensions [ 0 ] : " +  wellDimensions[ 0 ] );
		Utils.log( "Well dimensions [ 1 ] : " +  wellDimensions[ 1 ] );
	}

	private void configSites( ArrayList< File > files )
	{
		numSites = getNumSites( files );
		siteDimensions = new int[ 2 ];

		for ( int d = 0; d < siteDimensions.length; ++d )
		{
			siteDimensions[ d ] = ( int ) Math.ceil( Math.sqrt( numSites ) );
			siteDimensions[ d ] = Math.max( 1, siteDimensions[ d ] );
		}

		Utils.log( "Distinct sites: " +  numSites );
		Utils.log( "Site dimensions [ 0 ] : " +  siteDimensions[ 0 ] );
		Utils.log( "Site dimensions [ 1 ] : " +  siteDimensions[ 1 ] );
	}

	private int getNumSites( ArrayList< File > files )
	{
		Set< String > sites = new HashSet<>( );

		for ( File file : files )
		{
			final String pattern = Utils.getMultiPositionNamingScheme( file );

			final Matcher matcher = Pattern.compile( pattern ).matcher( file.getName() );

			if ( matcher.matches() )
			{
				if ( namingScheme.equals( Utils.PATTERN_ALMF_SCREENING_W0001_P000_C00 ) )
				{
					sites.add( matcher.group( 2 ) );
				}
			}
		}

		if ( sites.size() == 0 )
		{
			return 1;
		}
		else
		{
			return sites.size();
		}

	}

	private int[] getMaximalWellPositionsInData( ArrayList< File > files )
	{
		int[] maximalWellPosition = new int[ 2 ];

		for ( File file : files )
		{
			final Matcher matcher = Pattern.compile( namingScheme ).matcher( file.getName() );

			matcher.matches();

			String well = matcher.group( 1 );

			int[] wellPosition = new int[ 2 ];
			wellPosition[ 0 ] = Integer.parseInt( well.substring( 1, 3 ) ) - 1;
			wellPosition[ 1 ] = Utils.CAPITAL_ALPHABET.indexOf( well.substring( 0, 1 ) );

			for ( int d = 0; d < wellPosition.length; ++d )
			{
				if ( wellPosition[ d ] > maximalWellPosition[ d ] )
				{
					maximalWellPosition[ d ] = wellPosition[ d ];
				}
			}
		}

		return maximalWellPosition;

	}


	private FinalInterval getInterval( File file, int numWellColumns, int numSiteColumns )
	{
		String filePath = file.getAbsolutePath();

		final Matcher matcher = Pattern.compile( namingScheme ).matcher( filePath );

		if ( matcher.matches() )
		{
			int[] wellPosition = new int[ 2 ];
			int[] sitePosition = new int[ 2 ];

			int wellNum = Integer.parseInt( matcher.group( 1 ) ) - 1;
			int siteNum = Integer.parseInt( matcher.group( 2 ) );

			wellPosition[ 1 ] = wellNum / numWellColumns * numSiteColumns;
			wellPosition[ 0 ] = wellNum % numWellColumns * numSiteColumns;

			sitePosition[ 1 ] = siteNum / numSiteColumns;
			sitePosition[ 0 ] = siteNum % numSiteColumns;

			updateMaxWellDimensionInData( wellPosition );
			updateMaxSiteDimensionInData( sitePosition );

			final long[] min = computeMinCoordinates( wellPosition, sitePosition );
			final long[] max = new long[ min.length ];
			for ( int d = 0; d < min.length; ++d )
			{
				max[ d ] = min[ d ] + imageDimensions[ d ] - 1;
			}

			return new FinalInterval( min, max );

		}
		else
		{
			return null;
		}

	}

	private long[] computeMinCoordinates( int[] wellPosition, int[] sitePosition )
	{
		final long[] min = new long[ 2 ];

		for ( int d = 0; d < 2; ++d )
		{
			min[ d ] = wellPosition[ d ] + sitePosition[ d ];
			min[ d ] *= imageDimensions[ d ];
		}

		return min;
	}

	private void updateMaxWellDimensionInData( int[] wellPosition )
	{
		for ( int d = 0; d < 2; ++d )
		{
			if ( wellPosition[ d ] >= maxWellDimensionsInData[ d ] )
			{
				maxWellDimensionsInData[ d ] = wellPosition[ d ];
			}
		}
	}

	private void updateMaxSiteDimensionInData( int[] sitePosition )
	{
		for ( int d = 0; d < 2; ++d )
		{
			if ( sitePosition[ d ] >= maxSiteDimensionsInData[ d ] )
			{
				maxSiteDimensionsInData[ d ] = sitePosition[ d ];
			}
		}
	}

}