package de.embl.cba.plateviewer.filter;

import ij.gui.GenericDialog;
import ij.gui.NonBlockingGenericDialog;

public class ImageFilterUI
{

	public ImageFilterUI()
	{
	}

	public static ImageFilterSettings getImageFilterSettingsFromUI( ImageFilterSettings settings  )
	{
		if ( settings.filterType.equals( ImageFilter.SIMPLE_SEGMENTATION ) )
		{
			settings = simpleSegmentationUI( settings );
		}
		else if ( settings.filterType.equals( ImageFilter.MEDIAN_DEVIATION ) )
		{
			settings = medianDeviationUI( settings );
		}
		else if ( settings.filterType.equals( ImageFilter.INFORMATION ) )
		{
			settings = radiusUI( settings );
		}

		return settings;
	}

	private static ImageFilterSettings radiusUI( ImageFilterSettings settings )
	{
		final GenericDialog gd = new NonBlockingGenericDialog( settings.filterType );
		gd.addNumericField("Radius", settings.radius, 0, 5, "pixels" );
		gd.showDialog();
		if ( gd.wasCanceled() ) return null;
		settings.radius = (int) gd.getNextNumber();
		return settings;
	}


	private static ImageFilterSettings medianDeviationUI( ImageFilterSettings settings )
	{
		final GenericDialog gd = new NonBlockingGenericDialog(settings.filterType );
		gd.addNumericField("Radius", settings.radius , 0, 5, "pixels" );
		gd.addNumericField("Add", settings.offset, 0, 5, "gray values" );
		gd.addCheckbox("Divide by Sqrt(median)", settings.normalize );
		gd.addNumericField("Multiply by", settings.factor, 2, 5, "" );
		gd.showDialog();
		if ( gd.wasCanceled() ) return null;
		settings.radius = (int) gd.getNextNumber();
		settings.offset = (double) gd.getNextNumber();
		settings.normalize = gd.getNextBoolean();
		settings.factor = gd.getNextNumber();
		return settings;
	}


	private static ImageFilterSettings simpleSegmentationUI( ImageFilterSettings settings )
	{
		final GenericDialog gd = new NonBlockingGenericDialog( settings.filterType );
		gd.addNumericField("Threshold", settings.threshold ,
				0, 5, "gray values" );
		gd.addNumericField("Minimal object size",
				settings.minObjectSize, 0, 5, "pixels" );
		gd.showDialog();
		if ( gd.wasCanceled() ) return null;
		settings.threshold = (double ) gd.getNextNumber();
		settings.minObjectSize = (long) gd.getNextNumber();
		return settings;
	}

}
