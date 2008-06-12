uname_O := $(shell sh -c 'uname -o 2>/dev/null || echo not')
ifeq ($(uname_O),Cygwin)
CPSEP=\;
else
CPSEP=:
endif

PLUGINSDIR ?= ../plugins
JARSDIR ?= ../jars
EXTJARS=$(wildcard $(PLUGINSDIR)/*.jar) $(wildcard $(JARSDIR)/*.jar)
JAVACOPTSJARS=$(shell echo "$(EXTJARS)" | tr \  $(CPSEP))
JAVACOPTS=-classpath ../ij.jar$(CPSEP)$(JAVACOPTSJARS) -target 1.5 -source 1.5

JAVAS=$(wildcard \
    MOPS_ExtractPointRoi.java \
    SIFT_ExtractPointRoi.java \
    SIFT_align.java \
    Transform_Affine.java \
    Transform_Perspective.java \
    Transform_ElasticMesh.java \
    mpicbg/*/*.java)
CLASSES=$(patsubst %.java,%.class,$(JAVAS))
ALL_CLASSES=$(patsubst %.java,%*.class,$(JAVAS))
TARGET=mpicbg_.jar

all: $(TARGET)

show:
	echo $(ALL_CLASSES)

$(TARGET): plugins.config $(CLASSES)
	jar cvf $@ $< $(ALL_CLASSES)

$(CLASSES): %.class: %.java
	javac -O $(JAVACOPTS) $(JAVAS)

