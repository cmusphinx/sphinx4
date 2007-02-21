If this gui tool fails to run, please check the following things:

1. Make sure that these build.xml path variables lead to the correct directory :
	top_dir  
	build_dir (should be under top_dir), and 
	classes_dir (should be under build_dir)

2. util.conf configuration file should be in edu/cmu/sphinx/tools/gui directory, 
   together with other compiled GUI .class files

3. source_path in util.conf should be set to the directory where sphinx java 
   source codes (.java files) are located