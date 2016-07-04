PROTOC=../cpp/thirdparty/protobuf-2.5.0/src/protoc
PROTOBUF_INCLUDE=../cpp/thirdparty/protobuf-2.5.0/src

PBRPCCPP_PLUGIN=../bin/protoc-gen-pbrpccpp

TARGET_DIR_CPP_CLIENT=../cpp/generated/

PROTOS_FOUNDATION=$(wildcard pbrpc/*.proto) include/PBRPC.proto
PROTOS_SERVERS=$(wildcard xtreemfs/*.proto) include/Common.proto

.PHONY:

all: cpp

cpp:
	$(PROTOC) -I$(PROTOBUF_INCLUDE) -I. -I$(@D)/pbrpc/ $(PROTOS_FOUNDATION) --cpp_out=$(TARGET_DIR_CPP_CLIENT)

	$(PROTOC) include/Common.proto --cpp_out=$(TARGET_DIR_CPP_CLIENT)
	$(PROTOC) -I$(PROTOBUF_INCLUDE) -I. -I$(@D)/xtreemfs/ $(PROTOS_SERVERS) --cpp_out=$(TARGET_DIR_CPP_CLIENT)
	$(PROTOC) --plugin=$(PBRPCCPP_PLUGIN) -I$(PROTOBUF_INCLUDE) -I. -I$(@D)/xtreemfs/ $(PROTOS_SERVERS) --pbrpccpp_out=$(TARGET_DIR_CPP_CLIENT)
	$(PROTOC) --plugin=$(PBRPCCPP_PLUGIN) -I$(PROTOBUF_INCLUDE) -I. -I$(@D)/pbrpc/ $(PROTOS_FOUNDATION) --pbrpccpp_out=$(TARGET_DIR_CPP_CLIENT)
