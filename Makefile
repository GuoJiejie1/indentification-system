.PHONY: all clean doc compile

LIB_JARS=`find -L libs/ -name "*.jar" | tr [:space:] :`

compile:
	mkdir -p classes
	javac -sourcepath src/main/java -classpath $(LIB_JARS) -d classes `find -L src/ -name "*.java"`

doc:
	mkdir -p doc
	javadoc -sourcepath src -classpath $(LIB_JARS) -d doc peersim.kademlia

kad:
	java -Xmx500m -cp $(LIB_JARS):classes peersim.Simulator src/main/resources/kad.cfg

vrouter:
	java -Xmx500m -cp $(LIB_JARS):classes peersim.Simulator src/main/resources/vRouter.cfg

all: compile doc run

clean: 
	rm -fr classes doc