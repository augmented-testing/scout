// SPDX-FileCopyrightText: 2021 Michel Nass
//
// SPDX-License-Identifier: MIT

package plugin;

import scout.StateController;

public class ResetCoverage
{
	public void startTool()
	{
		StateController.resetCoverage();
	}
}
