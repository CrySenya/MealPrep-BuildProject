@echo off
if not exist out\mealprep\starter\gui\StarterGUI.class (
    echo Classes not found. Run build.bat first.
    exit /b 1
)
java -cp out mealprep.starter.gui.StarterGUI
