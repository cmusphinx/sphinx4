require 'rake/clean'

# Where to get Ruby and Rake:
# http://www.ruby-lang.org/en/
# http://rake.rubyforge.org/
# It is also possible to use JRuby with rake.

# Why you should use this instead of the Ant build or the SCons build
# More concise than Ant's hello world, debuggable (you can set a break point)
# Easy generate a dependencies, easy to interact with OS, easy to script.

# Usage:
# to build sphinx4.jar just type rake
# to remove class files:  rake clean
# to remove jars too:  rake clobber

SPHINX_JAR = 'sphinx4.jar'
JAVA_FILES = Dir['src/sphinx4/**/*.java']
BUILD_DIR = '../s4_build'
CLASS_DIR = "#{BUILD_DIR}/classes"

# Very easy to declare a dependency between all java files and a jar
file SPHINX_JAR => JAVA_FILES do
  mkdir_p CLASS_DIR
  `javac -cp lib/junit-4.4.jar -d #{CLASS_DIR} #{JAVA_FILES.join(" ")}`
  `jar cvf #{BUILD_DIR}/#{SPHINX_JAR} -C #{CLASS_DIR} .`
end

# Define, default, clobber, and clean tasks
task :default => SPHINX_JAR
CLOBBER.include BUILD_DIR
CLEAN.include CLASS_DIR