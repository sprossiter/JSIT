#!/bin/bash
# JSIT Linux bash script for model commits with automated versioning
# Author: Stuart Rossiter
#
#******************************************************************************       
#   Copyright 2015 University of Southampton
#        
#   This file is part of JSIT.
#
#   JSIT is free software: you can redistribute it and/or modify
#   it under the terms of the GNU Lesser General Public License as published by
#   the Free Software Foundation, either version 3 of the License, or
#   (at your option) any later version.
#
#   JSIT is distributed in the hope that it will be useful,
#   but WITHOUT ANY WARRANTY; without even the implied warranty of
#   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#   GNU Lesser General Public License for more details.
#
#   You should have received a copy of the GNU Lesser General Public License
#   along with JSIT.  If not, see <http://www.gnu.org/licenses/>.
#******************************************************************************
#
# Replace Sim/Libs with the path to the JSIT libraries directory.
# Replace Sim with the path to your model source directories (see the JSIT
# User Guide). This can include multiple directories separated by colons
# as in a standard Linux path. (Use double quotes if there are spaces.)
#
java -cp "Sim/Libs/*" \
uk.ac.soton.simulation.jsit.core.ModelVersioningAssistant COMMIT Sim
echo "-------------------------------------------------------"
echo "Commit complete (check for errors above)"
