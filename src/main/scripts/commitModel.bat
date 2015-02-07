@echo off
rem JSIT Windows batch script for model commits with automated versioning
rem Author: Stuart Rossiter
rem
rem******************************************************************************       
rem   Copyright 2015 University of Southampton
rem        
rem   This file is part of JSIT.
rem
rem   JSIT is free software: you can redistribute it and/or modify
rem   it under the terms of the GNU Lesser General Public License as published by
rem   the Free Software Foundation, either version 3 of the License, or
rem   (at your option) any later version.
rem
rem   JSIT is distributed in the hope that it will be useful,
rem   but WITHOUT ANY WARRANTY; without even the implied warranty of
rem   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
rem   GNU Lesser General Public License for more details.
rem
rem   You should have received a copy of the GNU Lesser General Public License
rem   along with JSIT.  If not, see <http://www.gnu.org/licenses/>.
rem******************************************************************************
rem
rem Replace Sim\Libs with the path to the JSIT libraries directory.
rem Replace Sim with the path to your model source directories (see the JSIT
rem User Guide). This can include multiple directories separated by semi-colons
rem as in a standard Windows path. (Use double quotes if there are spaces.)
rem
rem NB: There is a bug in some versions of Java pre 7u10 where the
rem wildcard expansion in the -cp bit doesn't work properly. (You get
rem a "failure to find main class in X" type error.) Upgrade your Java 
rem installation to 7u10 or beyond to fix it. See
rem http://superuser.com/questions/773660.
rem
java -cp "Sim\Libs\*" ^
uk.ac.soton.simulation.jsit.core.ModelVersioningAssistant COMMIT Sim
echo -------------------------------------------------------
echo Commit complete (check for errors above). Press any key
pause
