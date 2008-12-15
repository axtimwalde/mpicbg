uname_O := $(shell sh -c 'uname -o 2>/dev/null || echo not')
ifeq ($(uname_O),Cygwin)
CPSEP=\;
else
CPSEP=:
endif

PLUGINSDIR ?= ../plugins
JARSDIR ?= ../jars
EXTJARS=$(wildcard $(PLUGINSDIR)/*.jar) $(wildcard $(JARSDIR)/*.jar)
EXTJARSMINUSTARGET=$(subst $(PLUGINSDIR)/$(TARGET) ,,$(EXTJARS))
JAVACOPTSJARS=$(shell echo "$(EXTJARSMINUSTARGET)" | tr \  $(CPSEP))
JAVACOPTS=-classpath ../ij.jar$(CPSEP)$(JAVACOPTSJARS) -target 1.5 -source 1.5

JAVAS=$(wildcard \
    mpicbg/ij/*.java \
	mpicbg/imagefeatures/*.java \
    mpicbg/models/*.java \
    mpicbg/trakem2/*.java \
    mpicbg/util/*.java \
	Align_ElasticMeshStack.java \
	MOPS_ExtractPointRoi.java \
	Optic_Flow.java \
	SIFT_Align.java \
	SIFT_ExtractPointRoi.java \
	Transform_Affine.java \
	Transform_ElasticMovingLeastSquaresMesh.java \
	Transform_Grid.java \
	Transform_MovingLeastSquaresMesh.java \
	Transform_Perspective.java \
	Transform_Rigid.java \
	Transform_Similarity.java \
	Transform_Roi.java \
	Transform_SpringMesh.java \
	Transform_TriangularGrid.java)
RESOURCES ?= template.svg
CLASSES=$(patsubst %.java,%.class,$(JAVAS))
ALL_CLASSES=$(patsubst %.java,%*.class,$(JAVAS))
TARGET=mpicbg_.jar

all: $(TARGET)

show:
	echo $(ALL_CLASSES)

$(TARGET): plugins.config $(CLASSES)
	jar cvf $@ $< $(ALL_CLASSES) $(RESOURCES)

$(CLASSES): %.class: %.java
	javac -O $(JAVACOPTS) $(JAVAS)

clean:
	rm -f $(ALL_CLASSES)
