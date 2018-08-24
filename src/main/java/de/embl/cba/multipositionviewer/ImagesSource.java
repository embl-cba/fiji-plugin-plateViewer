package de.embl.cba.multipositionviewer;

import ij.IJ;
import ij.ImagePlus;
import net.imglib2.FinalInterval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImagesSource < T extends RealType< T > & NativeType< T > >
{

	private long[] dimensions;
	private int[] imageDimensions;
	private double[] lutMinMax;
	private int bitDepth;

	final ArrayList< File > files;
	final String filenamePattern;

	CachedCellImg< T, ? > cachedCellImg;

	MultiPositionLoader loader;

	final int numIoThreads;

	String name;

	public ImagesSource( ArrayList< File > files, String filenamePattern, int numIoThreads )
	{
		this.files = files;
		this.filenamePattern = filenamePattern;
		this.numIoThreads = numIoThreads;
		this.name = filenamePattern;

		setImageProperties();

		setMultiPositionLoader();

		setCachedCellImgDimensions();

		createCachedCellImg();
	}

	public String getName()
	{
		return name;
	}

	public void setName( String name )
	{
		this.name = name;
	}

	public void setCachedCellImgDimensions()
	{
		final ArrayList< ImageFile > imageFiles = loader.getImageFiles();

		FinalInterval union = new FinalInterval( imageFiles.get( 0 ).getInterval() );

		for ( ImageFile imageFile : imageFiles )
		{
			union = Intervals.union( imageFile.getInterval(), union );
		}

		// TODO: better making this smaller and with an offset...

		dimensions = new long[ 2 ];

		for ( int d = 0; d < 2; ++d )
		{
			dimensions[ d ] = union.max( d );
		}
	}

	private void setMultiPositionLoader()
	{
		loader = new MultiPositionLoader( files, filenamePattern, imageDimensions, bitDepth, numIoThreads );
	}

	public int getBitDepth()
	{
		return bitDepth;
	}

	public double[] getLutMinMax()
	{
		return lutMinMax;
	}

	public MultiPositionLoader getLoader()
	{
		return loader;
	}

	private void setImageProperties()
	{
		final ImagePlus imagePlus = getFirstImage();

		setImageBitDepth( imagePlus );

		setImageDimensions( imagePlus );

		setImageMinMax( imagePlus );
	}

	private void setImageMinMax( ImagePlus imagePlus )
	{
		lutMinMax = new double[ 2 ];
		lutMinMax[ 0 ] = imagePlus.getProcessor().getMin();
		lutMinMax[ 1 ] = imagePlus.getProcessor().getMax();
	}

	private ImagePlus getFirstImage()
	{
		return IJ.openImage( files.get( 0 ).getAbsolutePath() );
	}

	private void setImageBitDepth( ImagePlus imagePlus )
	{
		bitDepth = imagePlus.getBitDepth();
	}

	private void setImageDimensions( ImagePlus imagePlus )
	{
		imageDimensions = new int[ 2 ];
		imageDimensions[ 0 ] = imagePlus.getWidth();
		imageDimensions[ 1 ] = imagePlus.getHeight();
	}

	public CachedCellImg getCachedCellImg( )
	{
		return cachedCellImg;
	}


	private void createCachedCellImg()
	{
		switch ( bitDepth )
		{
			case 8:

				cachedCellImg = new ReadOnlyCachedCellImgFactory().create(
						dimensions,
						new UnsignedByteType(),
						loader,
						ReadOnlyCachedCellImgOptions.options().cellDimensions( imageDimensions ) );
				break;

			case 16:

				cachedCellImg = new ReadOnlyCachedCellImgFactory().create(
						dimensions,
						new UnsignedShortType(),
						loader,
						ReadOnlyCachedCellImgOptions.options().cellDimensions( imageDimensions ) );
				break;

			case 32:

				cachedCellImg = new ReadOnlyCachedCellImgFactory().create(
						dimensions,
						new UnsignedShortType(),
						loader,
						ReadOnlyCachedCellImgOptions.options().cellDimensions( imageDimensions ) );
				break;

			default:

				cachedCellImg = null;

		}

	}



	public static int getNumSites( ArrayList< File > files )
	{
		Set< String > sites = new HashSet<>( );

		for ( File file : files )
		{
			final String pattern = getPattern( file );

			final Matcher matcher = Pattern.compile( pattern ).matcher( file.getName() );

			if ( matcher.matches() )
			{
				if ( pattern.equals( Utils.PATTERN_ALMF_SCREENING_W0001_P000_C00 ) )
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

	public static int getNumWells( ArrayList< File > files )
	{
		Set< String > wells = new HashSet<>( );
		int maxWellNum = 0;

		for ( File file : files )
		{
			final String pattern = getPattern( file );

			final Matcher matcher = Pattern.compile( pattern ).matcher( file.getName() );

			if ( matcher.matches() )
			{
				wells.add( matcher.group( 1 ) );

				if ( pattern.equals( Utils.PATTERN_ALMF_SCREENING_W0001_P000_C00 ) )
				{
					int wellNum = Integer.parseInt( matcher.group( 1 ) );

					if ( wellNum > maxWellNum )
					{
						maxWellNum = wellNum;
					}
				}
			}
		}


		if ( maxWellNum > wells.size() )
		{
			return maxWellNum;
		}
		else
		{
			return wells.size();
		}

	}

	public static void putCellToMaps( ArrayList< Map< String, File > > cellFileMaps,
									  String cell,
									  File file )
	{
		boolean cellCouldBePlaceInExistingMap = false;

		for( int iMap = 0; iMap < cellFileMaps.size(); ++iMap )
		{
			if ( !cellFileMaps.get( iMap ).containsKey( cell ) )
			{
				cellFileMaps.get( iMap ).put( cell, file );
				cellCouldBePlaceInExistingMap = true;
				break;
			}
		}

		if ( ! cellCouldBePlaceInExistingMap )
		{
			// new channel
			cellFileMaps.add( new HashMap<>() );
			cellFileMaps.get( cellFileMaps.size() - 1 ).put( cell, file );
		}
	}

	public static String getPattern( File file )
	{
		String filePath = file.getAbsolutePath();

		if ( Pattern.compile( Utils.PATTERN_MD_A01_CHANNEL ).matcher( filePath ).matches() ) return Utils.PATTERN_MD_A01_CHANNEL;
		if ( Pattern.compile( Utils.PATTERN_ALMF_SCREENING_W0001_P000_C00 ).matcher( filePath ).matches() ) return Utils.PATTERN_ALMF_SCREENING_W0001_P000_C00;

		return Utils.PATTERN_NO_MATCH;
	}


	public int[] getCell( File file, String pattern, int numWellColumns, int numSiteColumns )
	{
		String filePath = file.getAbsolutePath();

		final Matcher matcher = Pattern.compile( pattern ).matcher( filePath );

		if ( matcher.matches() )
		{
			int[] wellPosition = new int[ 2 ];
			int[] sitePosition = new int[ 2 ];

			if ( pattern.equals( Utils.PATTERN_MD_A01_CHANNEL ) )
			{
				String well = matcher.group( 1 );

				wellPosition[ 0 ] = Integer.parseInt( well.substring( 1, 3 ) ) - 1;
				wellPosition[ 1 ] = Utils.CAPITAL_ALPHABET.indexOf( well.substring( 0, 1 ) );

			}
			else if ( pattern.equals( Utils.PATTERN_ALMF_SCREENING_W0001_P000_C00 ) )
			{

				int wellNum = Integer.parseInt( matcher.group( 1 ) );
				int siteNum = Integer.parseInt( matcher.group( 2 ) );

				wellPosition[ 1 ] = wellNum / numWellColumns * numSiteColumns;
				wellPosition[ 0 ] = wellNum % numWellColumns * numSiteColumns;

				sitePosition[ 1 ] = siteNum / numSiteColumns;
				sitePosition[ 0 ] = siteNum % numSiteColumns;

			}

//			updateMaxWellDimensionInData( wellPosition );
//			updateMaxSiteDimensionInData( sitePosition );

			final int[] cellPosition = computeCellPosition( wellPosition, sitePosition );

			return cellPosition;

		}
		else
		{
			return null;
		}

	}

	public int[] computeCellPosition( int[] wellPosition, int[] sitePosition )
	{
		final int[] cellPosition = new int[ 2 ];

		for ( int d = 0; d < 2; ++d )
		{
			cellPosition[ d ] = wellPosition[ d ] + sitePosition[ d ];
		}
		return cellPosition;
	}

//	public void updateMaxWellDimensionInData( int[] wellPosition )
//	{
//		for ( int d = 0; d < 2; ++d )
//		{
//			if ( wellPosition[ d ] >= maxWellDimensionsInData[ d ] )
//			{
//				maxWellDimensionsInData[ d ] = wellPosition[ d ];
//			}
//		}
//	}
//
//	public void updateMaxSiteDimensionInData( int[] sitePosition )
//	{
//		for ( int d = 0; d < 2; ++d )
//		{
//			if ( sitePosition[ d ] >= maxSiteDimensionsInData[ d ] )
//			{
//				maxSiteDimensionsInData[ d ] = sitePosition[ d ];
//			}
//		}
//	}


	private static long[] getDimensions( String plateType )
	{
		long[] dimensions = new long[ 2 ];

		switch ( plateType )
		{
			case Utils.WELL_PLATE_96:
				dimensions[ 0 ] = 12;
				dimensions[ 1 ] = 8;
				break;
			default:
				dimensions[ 0 ] = 12;
				dimensions[ 1 ] = 8;
		}

		return dimensions;
	}
}