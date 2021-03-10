package plugin;

import javax.swing.JOptionPane;

import scout.StateController;

public class RegisterLicenseKey
{
	public void startTool()
	{
		String licenseKey = (String) JOptionPane.showInputDialog(StateController.getParentFrame(), "Specify a License Key for Product ID: "+StateController.getProductId(), "Register License Key", JOptionPane.PLAIN_MESSAGE);
		if(licenseKey!=null && licenseKey.trim().length()>0)
		{
			if(StateController.registerLicenseKey(licenseKey))
			{
				StateController.displayMessage("License Key Registered");
			}
			else
			{
				StateController.displayMessage("Invalid License Key");
			}
		}
	}
}
