# Status bar
The status bar is an application that consists of a freely configurable layout. It will use a configuration file to set up the widgets correctly in the application. The application offers a grid with 40px by 40px cells with 8px gaps. The grid is always 3 cells high. The width depends on the screen resolution. There might be space left, that is not possible to put in the 40x40 grid. That will be filled with special widgets.

The widgets may have a different cell-based sizes. A widget may support different cell-based sizes. Widgets have to return their possible cell-based width and height combinations to the application. The configuration file will contain the position and size for each widget in the grid. If a widget stretches over multiple cells it will also stretch over the 8px gaps.

*Example*: A widget supports a width of 3 cells and a height of 1 cell. Its width will be 40px + 8px + 40px + 8px + 40px = 136px wide and 40px high. It needs to return this mode as width = 3 and height = 1.

The status bar application checks that the given configurations match with the available sizes of the widgets and the configured sizes and positions don't overlap or lie outside of the grid. The check for positioning are at least in y direction. In x direction the checks may be replaced with fallback behavior if the widgets reach out of the grid width to support changes of the display size.