Demo of using the class-based LM.

To compile, add the following to demo.xml:

    <target name="compile_sphinx_classbased">
	<mkdir dir="${classes_dir}/demo/sphinx/classbased"/>
	<javac debug="true"
	       source="1.4"
	       listfiles="true"
	       deprecation="true"
	       srcdir="${src_dir}"
	       includes="demo/sphinx/classbased/**"
	       destdir="${classes_dir}">
	    <classpath refid="libs"/>
	</javac>
	<copy file="${src_dir}/demo/sphinx/classbased/sample.trigram.lm"
	      todir="${classes_dir}/demo/sphinx/classbased"/>
	<copy file="${src_dir}/demo/sphinx/classbased/sample-with-classes.trigram.lm"
	      todir="${classes_dir}/demo/sphinx/classbased"/>
	<copy file="${src_dir}/demo/sphinx/classbased/uniform-classdefs"
	      todir="${classes_dir}/demo/sphinx/classbased"/>
	<copy file="${src_dir}/demo/sphinx/classbased/classbased.config.xml"
	      todir="${classes_dir}/demo/sphinx/classbased"/>
	<mkdir dir="${bin_dir}"/>
        <jar destfile="${bin_dir}/ClassBased.jar" 
	     manifest="${src_dir}/demo/sphinx/classbased/classbased.Manifest"
	     basedir="${classes_dir}"
	     includes="demo/sphinx/classbased/**"
	     filesonly="true"
	     compress="true"/>
    </target>


And run (in sphinx4 root): 
	ant -f demo.xml compile_sphinx_classbased

Then you should be able to execute:
	java -mx312m  -jar bin/ClassBased.jar

For questions: tanel.alumae@phon.ioc.ee

