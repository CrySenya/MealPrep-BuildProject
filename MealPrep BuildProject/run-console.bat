@echo off
if not exist out\mealprep\starter\StarterApp.class (
    echo Classes not found. Run build.bat first.
    exit /b 1
)
java -cp out mealprep.starter.StarterApp
