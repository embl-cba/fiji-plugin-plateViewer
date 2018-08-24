package de.embl.cba.multipositionviewer;

import bdv.util.Bdv;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvSource;
import bdv.util.volatiles.SharedQueue;
import bdv.util.volatiles.VolatileViews;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.UnsignedByteType;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class MultiPositionViewerUI extends JPanel implements ActionListener
{
	JFrame frame;
	JComboBox imageNamesComboBox;
	JComboBox imagesSourcesComboBox;
	JComboBox actionComboBox;

	final ArrayList< String > imageNames;
	final MultiPositionViewer multiPositionViewer;

	final ArrayList< BdvSource > addedSources;

	private static final String CONNECTED_COMPONENTS_ACTION = "Compute connected components";


	public MultiPositionViewerUI( ArrayList< String > imageNames, MultiPositionViewer multiPositionViewer )
	{
		this.imageNames = imageNames;
		this.multiPositionViewer = multiPositionViewer;
		this.addedSources = new ArrayList<>();

		addImageNamesComboBox( );

		addImageSourcesComboBox( );

		addActionComboBox( );

		createAndShowUI( );
	}

	private void addActionComboBox( )
	{
		actionComboBox = new JComboBox();
		actionComboBox.addItem( CONNECTED_COMPONENTS_ACTION );
		actionComboBox.addActionListener( this );
		add( actionComboBox );
	}

	private void addImageSourcesComboBox( )
	{
		imagesSourcesComboBox = new JComboBox();
		for( ImagesSource source : multiPositionViewer.getImagesSources() )
		{
			imagesSourcesComboBox.addItem( source.getName() );
		}
		imagesSourcesComboBox.addActionListener( this );
		add( imagesSourcesComboBox );
	}

	public void addImageNamesComboBox( )
	{
		imageNamesComboBox = new JComboBox();
		for ( String imageName : imageNames )
		{
			imageNamesComboBox.addItem( imageName );
		}
		imageNamesComboBox.addActionListener( this );
		add( imageNamesComboBox );
	}

	@Override
	public void actionPerformed( ActionEvent e )
	{
		if ( e.getSource() == imageNamesComboBox )
		{
			final String imageName = ( String ) imageNamesComboBox.getSelectedItem();
			multiPositionViewer.zoomToImage( imageName );
		}

		if ( e.getSource() == actionComboBox )
		{
			final int imagesSourceIndex = imagesSourcesComboBox.getSelectedIndex();
			final ImagesSource imagesSource = multiPositionViewer.getImagesSources().get( imagesSourceIndex );

			if ( actionComboBox.getSelectedItem().equals( CONNECTED_COMPONENTS_ACTION ) )
			{
				final CachedCellImg< BitType, ? > thresholdImg = createCachedThresholdImg( imagesSource );

				BdvSource bdvSource = addCachedImgToBdv( thresholdImg, multiPositionViewer );

				addedSources.add( bdvSource );

			}
		}

	}

	public static BdvSource addCachedImgToBdv( CachedCellImg< BitType, ? > thresholdImg, MultiPositionViewer multiPositionViewer )
	{
		Bdv bdv = multiPositionViewer.getBdv();
		SharedQueue loadingQueue = multiPositionViewer.getLoadingQueue();

		return BdvFunctions.show(
				VolatileViews.wrapAsVolatile( thresholdImg, loadingQueue ),
				"",
				BdvOptions.options().addTo( bdv ) );
	}

	public static CachedCellImg< BitType, ? > createCachedThresholdImg( ImagesSource imagesSource )
	{
		final CachedCellImg cachedCellImg = imagesSource.getCachedCellImg();

		double realThreshold = 1.0;

		int[] cellDimensions = new int[ cachedCellImg.getCellGrid().numDimensions() ];
		cachedCellImg.getCellGrid().cellDimensions( cellDimensions );

		final long[] imgDimensions = cachedCellImg.getCellGrid().getImgDimensions();

		return new ReadOnlyCachedCellImgFactory().create(
		imgDimensions,
		new UnsignedByteType(),
		new ThresholdLoader( cachedCellImg, realThreshold ),
		ReadOnlyCachedCellImgOptions.options().cellDimensions( cellDimensions )
);
	}

	/**
	 * Create the GUI and show it.  For thread safety,
	 * this method should be invoked from the
	 * event-dispatching thread.
	 */
	private void createAndShowUI( )
	{
		//Create and set up the window.
		frame = new JFrame( "Image Navigator" );
		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

		//Create and set up the content pane.
		setOpaque( true ); //content panes must be opaque
		setLayout( new BoxLayout(this, BoxLayout.Y_AXIS ) );

		frame.setContentPane( this );

		//Display the window.
		frame.pack();
		frame.setVisible( true );
	}

	private void refreshUI()
	{
		this.revalidate();
		this.repaint();
		frame.pack();
	}



}