package de.embl.cba.multipositionviewer;

import ij.IJ;
import ij.ImagePlus;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MultiPositionImagesSource
{

	int numSites, numWells;
	int[] siteDimensions;
	int[] wellDimensions;
	int[] maxWellDimensionsInData;
	int[] maxSiteDimensionsInData;

	private long[] dimensions;
	private int[] imageDimensions;
	private double[] lutMinMax;
	private int bitDepth;

	final ArrayList< File > files;
	final String fileNamePattern;
	final MultiPositionLoader loader;

	public MultiPositionImagesSource( ArrayList< File > files, String multipositionFilenamePattern  )
	{
		this.files = files;
		this.fileNamePattern = multipositionFilenamePattern;

		setImageProperties();

		this.loader = createMultiPositionLoader();

		this.maxWellDimensionsInData = new int[ 2 ];
		this.maxSiteDimensionsInData = new int[ 2 ];

	}

	private MultiPositionLoader createMultiPositionLoader()
	{
		MultiPositionLoader loader = new MultiPositionLoader( files, multipositionFilenamePattern, imageDimensions, bitDepth, numIoThreads );

		return loader;
	}

	public int getBitDepth()
	{
		return bitDepth;
	}

	public double[] getLutMinMax()
	{
		return lutMinMax;
	}

	public ImageFile getImageFile( int index )
	{
		return loader.getImageFile( index );
	}

	public ImageFile getImageFile( long[] coordinates )
	{
		return loader.getImageFile( index );
	}

	public void getImageCenterCoordinates( long[] imageCoordinates, int[] imageDimensions )
	{
		final AffineTransform3D affineTransform3D = getImageZoomTransform( imageCoordinates, imageDimensions );

		bdv.getBdvHandle().getViewerPanel().setCurrentViewerTransform( affineTransform3D );
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
		final String next = cellFileMap.keySet().iterator().next();
		File file = cellFileMap.get( next );
		return IJ.openImage( file.getAbsolutePath() );
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

		dimensions = new long[ 2 ];

		for ( int d = 0; d < 2; ++d )
		{
			dimensions[ d ] = imageDimensions[ d ] * wellDimensions[ d ] * siteDimensions[ d ];
		}
	}

	public CachedCellImg getCachedCellImg( )
	{
		switch ( bitDepth )
		{
			case 8:

				final CachedCellImg< UnsignedByteType, ? > byteTypeImg = new ReadOnlyCachedCellImgFactory().create(
						dimensions,
						new UnsignedByteType(),
						loader,
						ReadOnlyCachedCellImgOptions.options().cellDimensions( imageDimensions ) );
				return byteTypeImg;

			case 16:

				final CachedCellImg< UnsignedShortType, ? > unsignedShortTypeImg = new ReadOnlyCachedCellImgFactory().create(
						dimensions,
						new UnsignedShortType(),
						loader,
						ReadOnlyCachedCellImgOptions.options().cellDimensions( imageDimensions ) );
				return unsignedShortTypeImg;

			case 32:

				final CachedCellImg< FloatType, ? > floatTypeImg = new ReadOnlyCachedCellImgFactory().create(
						dimensions,
						new UnsignedShortType(),
						loader,
						ReadOnlyCachedCellImgOptions.options().cellDimensions( imageDimensions ) );
				return floatTypeImg;

			default:

				return null;

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

		if ( Pattern.compile( Utils.PATTERN_A01 ).matcher( filePath ).matches() ) return Utils.PATTERN_A01;
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

			if ( pattern.equals( Utils.PATTERN_A01 ) )
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

			updateMaxWellDimensionInData( wellPosition );
			updateMaxSiteDimensionInData( sitePosition );

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

	public void updateMaxWellDimensionInData( int[] wellPosition )
	{
		for ( int d = 0; d < 2; ++d )
		{
			if ( wellPosition[ d ] >= maxWellDimensionsInData[ d ] )
			{
				maxWellDimensionsInData[ d ] = wellPosition[ d ];
			}
		}
	}

	public void updateMaxSiteDimensionInData( int[] sitePosition )
	{
		for ( int d = 0; d < 2; ++d )
		{
			if ( sitePosition[ d ] >= maxSiteDimensionsInData[ d ] )
			{
				maxSiteDimensionsInData[ d ] = sitePosition[ d ];
			}
		}
	}


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