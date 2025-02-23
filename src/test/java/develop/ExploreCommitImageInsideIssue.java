package develop;

import de.embl.cba.plateviewer.github.SiteIssueRaiser;
import de.embl.cba.plateviewer.io.JpegOutputStreamWriter;
import de.embl.cba.tables.github.GitHubIssue;
import ij.IJ;
import ij.ImagePlus;

public class ExploreCommitImageInsideIssue
{
	public static void main( String[] args )
	{
		final ImagePlus imp = IJ.openImage( "/Users/tischer/Documents/fiji-plugin-plateViewer/src/test/resources/ALMF-EMBL-JPEG/P001--A1--A1/D0004BS000000007-1uM--A1--A1--W0001--P001--T00001--Z001--C01.ome.jpeg" );

		final String base64String = JpegOutputStreamWriter.createBase64String( imp, 1.0F );

		final String a0 = "2309df0a4fbbf32b9b3";
		final String accessToken = a0 + "cdbb4ca946162378ee4a5";

		final SiteIssueRaiser siteIssueRaiser = new SiteIssueRaiser();

		final GitHubIssue gitHubIssue = new GitHubIssue(
				"Test embed image",
				"<img src=\"data:image/png;base64," + base64String + "\">",
				new String[]{"test"} );

		final GitHubIssue issue = gitHubIssue;

		siteIssueRaiser.postIssue(
				"https://github.com/hci-unihd/antibodies-analysis-issues",
				accessToken,
				gitHubIssue);
	}
}
