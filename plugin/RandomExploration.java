package plugin;

import scout.StateController;

public class RandomExploration
{
	public void enablePlugin()
	{
		StateController.setSystemProperty("weight_factor", "0");
		StateController.setSystemProperty("coverage_factor", "0");
		StateController.setSystemProperty("random_factor", "1");
		StateController.displayMessage("Random exploration enabled");
	}

	public void disablePlugin()
	{
		StateController.setSystemProperty("weight_factor", "1");
		StateController.setSystemProperty("coverage_factor", "1");
		StateController.setSystemProperty("random_factor", "0");
		StateController.displayMessage("Random exploration disabled");
	}
}
