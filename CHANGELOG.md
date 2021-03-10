# Changelog

## Version 3.3

- Dialog for editing widget identifiers.
- Tools menu.
- Reset button replaced by ResetCoverage tool.
- Help tool.

## Version 3.2

- Improved widget locators.
- Updated toolbar menu.
- New Chrome drivers.
- Improved auto repair.
- Automatically go home during autopilot.

## Version 2.9

- Inserts, checks and repairs neighbors.
- Improved multi-locators.
- RemoteControl plugin contains the run command that executes the latest path in the background.

## Version 2.8

- AutoRepair plugin.
- Faster auto execution.

## Version 2.7

- Faster multi-locators.
- Select product dialog.
- Generate multi-locator Java source including cookies.
- Displays both state and page coverage.

## Version 2.6

- Mimic plugin.
- New method updateClassifications for updating classification on widgets.
- Plugins are displayed in two rows.
- Press the 'c' button to cut a widget without removing the children.

## Version 2.5

- Move a widget using drag-and-drop.
- A border is displayed around the selected widget.
- GenerateJavaSource plugin that generates Java source code from the latest session.
- Autopilot waits - seconds before repairing a widget (only if it does not affect a locator).
- Improved handling of mouse events.
- Cookies are stored inside the model.
- Long clicks are animated.
- Mutants to a repaired widget are also repaired.

## Version 2.4

- Improved detection of widget location.
- Adds an action to the model only if there is a change.
- More reliable left click.
- Long click to go to the href specified by a widget.
- Accept Cookies option in the Bookmarks menu.
- Improved menu.
- The "{CurrentPath}" is replaced by the current directory in the home locator.

## Version 2.3

- Improved multi-locators, checks neighbor widgets.
- Suggest checks.
- Gödel Test report plugin.

## Version 2.2

- Support for animating plugins.
- AnimateAction plugin.
- Improved performance.
- Fixed auto correction.

## Version 2.1

- Auto correct and ignore widget metadata for all clones.
- Set product property "home_restart_session" to "yes" to restart session on the go home action.
- Set product property "elements_to_extract" to the HTML elements to extract (comma separated tags).
- Set product property "click_classes" to a space separated list of class names that should be identified as click elements.
- Set product property "select_classes" to a space separated list of class names that should be identified as select elements.
- Set product property "check_classes" to a space separated list of class names that should be identified as check elements.
- Set product property "type_classes" to a space separated list of class names that should be identified as type elements.

## Version 2.0

- Use less resources when the tester and SUT is idle.
- Improved toolbar.
- The text to verify is displayed before adding a check.
- Application title is customizable using system properties (application_title).

## Version 1.9

- Toolbar replaced by AugmentToolbar plugin.
- Faster click (does not wait for a double-click).
- Pause and Resume have been removed (can be restored by editing the AugmentToolbar plugin).
- Go Home button in the toolbar.
- The Auto button clears the coverage if 100%.

## Version 1.8

- getLastCapture method in PluginController.
- processCapture method available for modifying the capture.
- WidgetMetadata plugin that augments the metadata of a widget.
- Gray plugin that produces a grayscale image from the capture.

## Version 1.7

- SessionReports and IssueReports use a filename compatible with Ubuntu.
- ManualInstruction overwrites the current instruction, if needed.
- Plugins may augment the default background image.
- Setup program for Scout.

## Version 1.6

- Improved comments in the AugmentState and SeleniumPlugin plugins.

## Version 1.5

- Improved AugmentState plugin.
- EasterEgg plugin.

## Version 1.4

- SuggestClones plugin now support type, select and gohome widgets.
- HelloWorld example plugin.

## Version 1.3

- Enable/disable plugins from Scout.
- Drop-down for reports.
- Support for bookmarks.
- Improved augmentation.
- Manual instruction plugin.
- Support for password fields in the Selenium plugin.

## Version 1.2

- Improved handling of scroll wheel.
- Fixed resize window problem.

## Version 1.1

- Improved strategy for selecting the recommended widget.
- Suggestions are no longer recommended.
- Improved and simplified API.
- Arrows indicate multiple visible widgets.

## Version 1.0

- Expressions for validating checks.
- Modify expressions using the keyboard.
- Cookies stored per state.

## Version 0.9

- Improved and simplified handling of suggestions.
- Checks and clicks in frames.
- Improved drag and drop.
- Improved selection of widgets.
- Fixed defect in the autopilot plugin.
- Add a comment or report an issue using the enter key.

## Version 0.8

- Improved detection of checks (Selenium plugin).

## Version 0.7

- Improved widget recognition (Selenium plugin).
- More options in start session dialog.

## Version 0.6

- Support for reports.
- Plugin that report all sessions in the current product ## Version.
- Plugin that report all issues.

## Version 0.5

- Report issues.
- Navigate to issues.
- Help instructions.

## Version 0.4

- All drawing is performed by plugins.
- Actions are created by Scout and performed by plugins.
- Faster typing of text.
- No popups.

## Version 0.3

- Type a text directly using the keyboard.
- More stable autopilot.
- Suggestion plugins only give the suggestions one time.

## Version 0.2

- Support for suggestions.
- Boundary value suggestions plugin.
- Plugin that suggests clones.
- Random mission plugin.
- Possible to enter many values into the same text input field.

## Version 0.1

- Contains basic features for recording a session and auto executing a recorded session.
- Support buttons and text input fields but not drop-downs and lists.
