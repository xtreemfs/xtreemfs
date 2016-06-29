PROTOC=../cpp/thirdparty/protobuf-2.5.0/src/protoc
BASE_DIR=$(shell pwd)
INCLUDE_DIR=include/
PROTOBUF_INCLUDE=../cpp/thirdparty/protobuf-2.5.0/src

PBRPC_PLUGIN=../bin/protoc-gen-pbrpc
PBRPCCPP_PLUGIN=../bin/protoc-gen-pbrpccpp

TARGET_DIR_JAVA_FOUNDATION=../java/foundation/src/main/java
GENERATED_DIR_JAVA_FOUNDATION=$(TARGET_DIR_JAVA_FOUNDATION)/org/xtreemfs/foundation/pbrpc/generatedinterfaces/

TARGET_DIR_JAVA_PBRPCGEN=../java/pbrpcgen/src/main/java
GENERATED_DIR_JAVA_PBRPCGEN=$(TARGET_DIR_JAVA_PBRPCGEN)/org/xtreemfs/foundation/pbrpc/generatedinterfaces/

TARGET_DIR_JAVA_SERVERS=../java/servers/src/main/java
GENERATED_DIR_JAVA_SERVERS=$(TARGET_DIR_JAVA_SERVERS)/org/xtreemfs/pbrpc/generatedinterfaces/

TARGET_DIR_CPP_CLIENT=../cpp/generated/
TARGET_DIR_H_CLIENT=../client/include/generated/


PROTOS_FOUNDATION=$(wildcard pbrpc/*.proto) include/PBRPC.proto
PROTOS_SERVERS=$(wildcard xtreemfs/*.proto) include/Common.proto

.PHONY: foundation_clean pbrpcgen_clean servers_clean

all: cpp servers foundation

cpp:
	$(PROTOC) -I$(PROTOBUF_INCLUDE) -I. -I$(@D)/pbrpc/ $(PROTOS_FOUNDATION) --cpp_out=$(TARGET_DIR_CPP_CLIENT)

	$(PROTOC) include/Common.proto --cpp_out=$(TARGET_DIR_CPP_CLIENT)
	$(PROTOC) -I$(PROTOBUF_INCLUDE) -I. -I$(@D)/xtreemfs/ $(PROTOS_SERVERS) --cpp_out=$(TARGET_DIR_CPP_CLIENT)
	$(PROTOC) --plugin=$(PBRPCCPP_PLUGIN) -I$(PROTOBUF_INCLUDE) -I. -I$(@D)/xtreemfs/ $(PROTOS_SERVERS) --pbrpccpp_out=$(TARGET_DIR_CPP_CLIENT)
	$(PROTOC) --plugin=$(PBRPCCPP_PLUGIN) -I$(PROTOBUF_INCLUDE) -I. -I$(@D)/pbrpc/ $(PROTOS_FOUNDATION) --pbrpccpp_out=$(TARGET_DIR_CPP_CLIENT)


servers:
	$(PROTOC) include/Common.proto --java_out=$(TARGET_DIR_JAVA_SERVERS)
	$(PROTOC) -I$(PROTOBUF_INCLUDE) -I. -I$(@D)/xtreemfs/ $(PROTOS_SERVERS) --java_out=$(TARGET_DIR_JAVA_SERVERS)
	$(PROTOC) --plugin=$(PBRPC_PLUGIN) -I$(PROTOBUF_INCLUDE) -I. -I$(@D)/xtreemfs/ $(PROTOS_SERVERS) --pbrpc_out=$(TARGET_DIR_JAVA_SERVERS)

servers_clean:
	@rm -r $(GENERATED_DIR_JAVA_SERVERS)/*.java

foundation: pbrpc/*.proto
	$(PROTOC) -I$(PROTOBUF_INCLUDE) -I. -I$(@D)/pbrpc/ $(PROTOS_FOUNDATION) --java_out=$(TARGET_DIR_JAVA_FOUNDATION)
	$(PROTOC) --plugin=$(PBRPC_PLUGIN) -I$(PROTOBUF_INCLUDE) -I. -I$(@D)/pbrpc/ $(PROTOS_FOUNDATION) --pbrpc_out=$(TARGET_DIR_JAVA_FOUNDATION)

foundation_clean:
	@rm -r $(GENERATED_DIR_JAVA_FOUNDATION)/*.java

pbrpcgen: include/PBRPC.proto
	$(PROTOC) -I$(PROTOBUF_INCLUDE) -Iinclude/ $< --java_out=$(TARGET_DIR_JAVA_PBRPCGEN)

pbrpcgen_clean:
	@rm -r $(GENERATED_DIR_JAVA_PBRPCGEN)/*.java
