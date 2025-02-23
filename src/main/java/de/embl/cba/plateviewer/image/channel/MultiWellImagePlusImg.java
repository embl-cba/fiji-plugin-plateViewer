package de.embl.cba.plateviewer.image.channel;

import de.embl.cba.plateviewer.image.cellloader.MultiSiteImagePlusLoader;
import de.embl.cba.plateviewer.image.MultiWellChannelFilesProviderFactory;
import de.embl.cba.tables.color.ColorUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.process.LUT;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;

import java.awt.*;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.util.List;

public class MultiWellImagePlusImg< T extends RealType< T > & NativeType< T > > extends MultiWellImg< T >
{
	public MultiWellImagePlusImg( List< File > files, String channelName, String namingScheme, int resolutionLevel )
	{
		super( files, namingScheme, resolutionLevel, channelName );

		setImagePlusProperties( files.get( 0 ) );

		multiWellChannelFilesProvider = MultiWellChannelFilesProviderFactory.getMultiWellChannelFilesProvider( files, namingScheme, imageDimensions );

		singleSiteChannelFiles = multiWellChannelFilesProvider.getSingleSiteChannelFiles();

		setCachedCellImgDimensions( singleSiteChannelFiles );

		wellNames = multiWellChannelFilesProvider.getWellNames();

		loader = new MultiSiteImagePlusLoader( singleSiteChannelFiles );

		setCachedCellImg();
	}

	private void setImagePlusProperties( File file )
	{
		final ImagePlus imagePlus = IJ.openImage( file.getAbsolutePath() );

		setLut( imagePlus );

		setImageDataType( imagePlus );

		setImageDimensions( imagePlus );
	}

	private void setLut( ImagePlus imagePlus )
	{
		setLutColor( imagePlus );

		setLutMinMax( imagePlus );

		isInitiallyVisible = true;
	}

	private void setLutColor( ImagePlus imagePlus )
	{
		final String title = imagePlus.getTitle().toLowerCase();

		if ( title.contains( "gfp" ) )
			argbType = new ARGBType( ARGBType.rgba( 0, 255, 0, 255 ) );
		else if ( title.contains( "mcherry" ) )
			argbType = new ARGBType( ARGBType.rgba( 255, 0, 0, 255 ) );
		else if ( title.contains( "dapi" ) )
			argbType = new ARGBType( ARGBType.rgba( 0, 0, 255, 255 ) );
		else if ( title.contains( "hoechst" ) )
			argbType = new ARGBType( ARGBType.rgba( 0, 0, 255, 255 ) );
		else if ( title.contains( "yfp" ) )
			argbType = new ARGBType( ARGBType.rgba( 255, 255, 0, 255 ) );
		else if ( title.contains( "cfp" ) )
			argbType = new ARGBType( ARGBType.rgba( 0, 255, 255, 255 ) );
		else if ( title.contains( "rfp" ) )
			argbType = new ARGBType( ARGBType.rgba( 255, 0, 0, 255 ) );
		else if ( title.contains( "a488" ) )
			argbType = new ARGBType( ARGBType.rgba( 0, 255, 0, 255 ) );
		else if ( title.contains( "alexa488" ) )
			argbType = new ARGBType( ARGBType.rgba( 0, 255, 0, 255 ) );
		else if ( title.contains( "c00.ome.tif" ) )
			argbType = new ARGBType( ARGBType.rgba( 0, 0, 255, 255 ) );
		else if ( title.contains( "c01.ome.tif" ) )
			argbType = new ARGBType( ARGBType.rgba( 0, 255, 0, 255 ) );
		else if ( title.contains( "c02.ome.tif" ) )
			argbType = new ARGBType( ARGBType.rgba( 255, 0, 255, 255 ) );
		else
		{
			final LUT[] luts = imagePlus.getLuts();

			if ( luts.length > 0 )
			{
				final LUT lut = luts[ 0 ];
				final IndexColorModel colorModel = lut.getColorModel();
				final int mapSize = colorModel.getMapSize();
				final int red = colorModel.getRed( mapSize - 1 );
				final int green = colorModel.getRed( mapSize - 1 );
				final int blue = colorModel.getRed( mapSize - 1 );

				final int rgba = ARGBType.rgba( red, green, blue, 255 );
				argbType = new ARGBType( rgba );
			}
			else
			{
				argbType = ColorUtils.getARGBType( Color.WHITE );
			}
		}
	}

	private void setLutMinMax( ImagePlus imagePlus )
	{
		contrastLimits = new double[ 2 ];
		contrastLimits[ 0 ] = imagePlus.getProcessor().getMin();
		contrastLimits[ 1 ] = imagePlus.getProcessor().getMax();
	}

	private void setImageDataType( ImagePlus imagePlus )
	{
		int bitDepth = imagePlus.getBitDepth();

		switch ( bitDepth )
		{
			case 8:
				nativeType = new UnsignedByteType();
				break;
			case 16:
				nativeType = new UnsignedShortType();
				break;
			case 24: // RGB: currently returns sum of all three RGB values
				nativeType = new UnsignedShortType();
				break;
			case 32:
				nativeType = new FloatType();
				break;
			default:
				nativeType = null;
		}
	}

	private void setImageDimensions( ImagePlus imagePlus )
	{
		imageDimensions = new int[ 2 ];
		imageDimensions[ 0 ] = imagePlus.getWidth();
		imageDimensions[ 1 ] = imagePlus.getHeight();
	}
}
