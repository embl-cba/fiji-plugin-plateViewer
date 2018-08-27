package de.embl.cba.multipositionviewer;

import bdv.util.*;
import bdv.util.volatiles.VolatileViews;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

public class SimpleSegmentation
{
	final ImagesSource imagesSource;
	final double threshold;
	final long minObjectSize;
	final MultiPositionViewer multiPositionViewer;

	private BdvSource segmentationBdvSource;
	private SimpleSegmentationLoader< UnsignedByteType > loader;

	public SimpleSegmentation( ImagesSource imagesSource, double threshold, long minObjectSize, MultiPositionViewer multiPositionViewer )
	{
		this.imagesSource = imagesSource;
		this.threshold = threshold;
		this.minObjectSize = minObjectSize;
		this.multiPositionViewer = multiPositionViewer;

		final CachedCellImg< UnsignedByteType, ? > cachedCellImg = createCachedCellImg();

		addCachedCellImgToViewer( cachedCellImg );

	}

	public void addCachedCellImgToViewer( CachedCellImg< UnsignedByteType, ? > cachedCellImg )
	{

		segmentationBdvSource = BdvFunctions.show(
				VolatileViews.wrapAsVolatile( cachedCellImg, multiPositionViewer.getLoadingQueue() ),
				"segmentation",
				BdvOptions.options().addTo( multiPositionViewer.getBdv() ) );

		segmentationBdvSource.setColor( new ARGBType( ARGBType.rgba( 0, 255,0,255 )));
	}

	public CachedCellImg< UnsignedByteType, ? > createCachedCellImg( )
	{
		final CachedCellImg cachedCellImg = imagesSource.getCachedCellImg();

		int[] cellDimensions = new int[ cachedCellImg.getCellGrid().numDimensions() ];
		cachedCellImg.getCellGrid().cellDimensions( cellDimensions );

		final long[] imgDimensions = cachedCellImg.getCellGrid().getImgDimensions();

		loader = new SimpleSegmentationLoader(
				imagesSource,
				threshold,
				minObjectSize,
				multiPositionViewer.getBdv() );

		return new ReadOnlyCachedCellImgFactory().create(
				imgDimensions,
				new UnsignedByteType(),
				loader,
				ReadOnlyCachedCellImgOptions.options().cellDimensions( cellDimensions ) );
	}


	public void dispose()
	{
		segmentationBdvSource.removeFromBdv();
		segmentationBdvSource = null;
		loader.dispose();
		loader = null;
	}
}