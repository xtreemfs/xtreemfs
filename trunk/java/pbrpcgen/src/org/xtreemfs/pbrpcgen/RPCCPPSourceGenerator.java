package org.xtreemfs.pbrpcgen;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import google.protobuf.compiler.Plugin.CodeGeneratorRequest;
import google.protobuf.compiler.Plugin.CodeGeneratorResponse;
import google.protobuf.compiler.Plugin.CodeGeneratorResponse.File;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.ExtensionRegistry;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.PBRPC;

public class RPCCPPSourceGenerator {

    private static final Map<Type,String> ttt;

    private static final Map<Type,String> tttr;

    static {
        ttt = new HashMap();
        ttt.put(Type.TYPE_BOOL,"boolean");
        ttt.put(Type.TYPE_BYTES,"ByteString");
        ttt.put(Type.TYPE_DOUBLE,"double");
        ttt.put(Type.TYPE_FIXED32,"boost::uint32_t");
        ttt.put(Type.TYPE_FIXED64,"boost:uint64_t");
        ttt.put(Type.TYPE_FLOAT,"float");
        ttt.put(Type.TYPE_STRING,"std::string");

        tttr = new HashMap();
        tttr.put(Type.TYPE_BOOL,"List<Boolean>");
        tttr.put(Type.TYPE_BYTES,"List<ByteString>");
        tttr.put(Type.TYPE_DOUBLE,"List<Double>");
        tttr.put(Type.TYPE_FIXED32,"List<Integer>");
        tttr.put(Type.TYPE_FIXED64,"List<Long>");
        tttr.put(Type.TYPE_FLOAT,"List<Float>");
        tttr.put(Type.TYPE_STRING,"List<String>");
    }

    //public static final String PACKAGE = "org.xtreemfs.foundation.pbrpc.generated";

    public static void main(String[] args) throws Exception {
        final String ONE_INDENT = "    ";

        ExtensionRegistry er = ExtensionRegistry.newInstance();
        PBRPC.registerAllExtensions(er);
        CodeGeneratorRequest rq = CodeGeneratorRequest.parseFrom(System.in,er);
        CodeGeneratorResponse.Builder responseBuilder = CodeGeneratorResponse.newBuilder();

        Map<String,TypeDef> typeDefs = new HashMap();


        Map<FieldDescriptor, Object> map = rq.getAllFields();
        // System.err.println(map);

        for (Entry<FieldDescriptor, Object> files : map.entrySet()) {

            if (files.getKey().getName().equals("proto_file")) {

                for (FileDescriptorProto proto : (List<FileDescriptorProto>) files.getValue()) {
                    java.io.File filchen = new java.io.File(proto.getName());
                    System.err.println("proto name: "+proto.getName());
                    final String fileName = proto.getName().replace(".proto", ".pb.h");
                    System.err.println("include: "+fileName);
                    String pkgName = proto.getPackage();
                    if (pkgName.length() > 0)
                        pkgName = pkgName+".";
                    for (DescriptorProto msg : proto.getMessageTypeList()) {
                        TypeDef def = new TypeDef();
                        def.fullName = proto.getPackage().replace(".", "::")+"::"+msg.getName();
                        def.message = msg;
                        def.fileName = fileName;
                        typeDefs.put("."+pkgName+msg.getName(),def);
                    }
                    for (EnumDescriptorProto msg : proto.getEnumTypeList()) {
                        TypeDef def = new TypeDef();
                        def.fullName = proto.getPackage().replace(".", "::")+"::"+msg.getName();
                        def.message = null;
                        def.fileName = fileName;
                        typeDefs.put("."+pkgName+msg.getName(),def);
                    }
                }
            }
        }
        System.err.println("typedefs: "+typeDefs.keySet());

        Set<String> includes = new HashSet();

        for (Entry<FieldDescriptor, Object> files : map.entrySet()) {

            if (files.getKey().getName().equals("proto_file")) {

                for (FileDescriptorProto proto : (List<FileDescriptorProto>) files.getValue()) {

                    final String cppPackage = proto.getPackage();
                    System.err.println("package: "+cppPackage);
                    //final String filePrefix = cppPackage.replace(".", "/");
                    java.io.File filchen = new java.io.File(proto.getName());
                    final String msgName = filchen.getName().replace(".proto", "");
                    System.err.println("proto: "+proto.getName());


                    //printMessage(proto.getMessageTypeList());

                    for (ServiceDescriptorProto srv : proto.getServiceList()) {

                        String className = srv.getName() + "Client";

                        StringBuilder codeBuilder = new StringBuilder();
                        
                        //imports
                        codeBuilder.append("//automatically generated from "+filchen.getName()+" at "+new Date()+"\n");
                        codeBuilder.append("//(c) "+((new Date()).getYear()+1900)+". See LICENSE file for details.\n\n");

                        codeBuilder.append("#ifndef "+className.toUpperCase()+"_H\n");
                        codeBuilder.append("#define "+className.toUpperCase()+"_H\n\n");

                        codeBuilder.append("#include <boost/cstdint.hpp>\n");
                        codeBuilder.append("#include \"pbrpc/RPC.pb.h\"\n");
                        codeBuilder.append("#include \"rpc/client.h\"\n");
                        codeBuilder.append("#include \"rpc/sync_callback.h\"\n");
                        codeBuilder.append("#include \"rpc/callback_interface.h\"\n");
                        codeBuilder.append("@@@INCLUDE@@@\n");

                        codeBuilder.append("\n");
                        
                        String[] namespaceTokens = cppPackage.split("\\.");
                        String indent = "";
                        for (int i = 0; i < namespaceTokens.length; i++) {
                            codeBuilder.append(indent);
                            codeBuilder.append("namespace "+namespaceTokens[i]+" {\n");
                            indent = indent + ONE_INDENT;
                        }

                        codeBuilder.append(indent+"using ::xtreemfs::rpc::Client;\n");
                        codeBuilder.append(indent+"using ::xtreemfs::rpc::CallbackInterface;\n");
                        codeBuilder.append(indent+"using ::xtreemfs::rpc::SyncCallback;\n\n");

                        codeBuilder.append(indent+"class " + className + " {\n\n");
                        codeBuilder.append(indent+"public:\n");

                        //methods

                        codeBuilder.append(indent+ONE_INDENT+className+"(Client* client) : client_(client) {\n");
                        codeBuilder.append(indent+ONE_INDENT+"}\n\n");

                        codeBuilder.append(indent+ONE_INDENT+"virtual ~"+className+"() {\n");
                        codeBuilder.append(indent+ONE_INDENT+"}\n\n");

                        int interfaceId = srv.getOptions().getExtension(PBRPC.interfaceId);

                        for (MethodDescriptorProto method: srv.getMethodList()) {

                            System.err.println("input type: "+method.getInputType());
                            final String inputType = typeDefs.get(method.getInputType()).fullName;//msgName+method.getInputType();

                            final String returnType = typeDefs.get(method.getOutputType()).fullName;//msgName+method.getOutputType();

                            includes.add(typeDefs.get(method.getInputType()).fileName);
                            includes.add(typeDefs.get(method.getOutputType()).fileName);

                            final int procId = method.getOptions().getExtension(PBRPC.procId);

                            final boolean data_in = method.getOptions().hasExtension(PBRPC.dataIn) ? method.getOptions().getExtension(PBRPC.dataIn) : false;
                            final String dataValue = data_in ? "data" : "null";

                            String returnTypeBuilder = "new "+returnType+"()";
                            if (returnType.contains("emptyResponse"))
                                returnTypeBuilder = "NULL";

                            codeBuilder.append(indent+ONE_INDENT+"void "+method.getName()+"(const std::string &address,\n");
                            codeBuilder.append(indent+ONE_INDENT+ONE_INDENT+"const xtreemfs::pbrpc::Auth& auth,\n");
                            codeBuilder.append(indent+ONE_INDENT+ONE_INDENT+"const xtreemfs::pbrpc::UserCredentials &creds,");
                            if (!inputType.contains("emptyRequest"))
                                codeBuilder.append("\n"+indent+ONE_INDENT+ONE_INDENT+"const "+inputType+"* request,");
                            if (data_in)
                                codeBuilder.append("const char* data, boost::uint32_t data_length,");
                            codeBuilder.append("\n"+indent+ONE_INDENT+ONE_INDENT+"CallbackInterface<"+returnType+"> *callback, void *context = NULL) {\n");


                            if (!data_in)
                                codeBuilder.append(indent+ONE_INDENT+ONE_INDENT+"const char* data = NULL; boost::uint32_t data_length = 0;\n");
                            if (inputType.contains("emptyRequest"))
                                codeBuilder.append(indent+ONE_INDENT+ONE_INDENT+inputType+"* request = NULL;\n");

                            codeBuilder.append(indent+ONE_INDENT+ONE_INDENT+"client_->sendRequest(address, "+interfaceId+", "+procId+",\n");
                            codeBuilder.append(indent+ONE_INDENT+ONE_INDENT+ONE_INDENT+" creds, auth, request, data, data_length, "+returnTypeBuilder+",\n");
                            codeBuilder.append(indent+ONE_INDENT+ONE_INDENT+ONE_INDENT+" context, callback);\n");
                            codeBuilder.append(indent+ONE_INDENT+"}\n\n");


                            codeBuilder.append(indent+ONE_INDENT+"SyncCallback<"+returnType+">* "+method.getName()+"_sync(const std::string &address,\n");
                            codeBuilder.append(indent+ONE_INDENT+ONE_INDENT+"const xtreemfs::pbrpc::Auth& auth,\n");
                            codeBuilder.append(indent+ONE_INDENT+ONE_INDENT+"const xtreemfs::pbrpc::UserCredentials &creds");
                            if (!inputType.contains("emptyRequest"))
                                codeBuilder.append("\n"+indent+ONE_INDENT+ONE_INDENT+", const "+inputType+"* request");
                            if (data_in)
                                codeBuilder.append(", const char* data, boost::uint32_t data_length");
                            codeBuilder.append(") {\n");


                            if (!data_in)
                                codeBuilder.append(indent+ONE_INDENT+ONE_INDENT+"const char* data = NULL; boost::uint32_t data_length = 0;\n");
                            if (inputType.contains("emptyRequest"))
                                codeBuilder.append(indent+ONE_INDENT+ONE_INDENT+inputType+"* request = NULL;\n");

                            codeBuilder.append(indent+ONE_INDENT+ONE_INDENT+"SyncCallback<"+returnType+">* sync_cb = new SyncCallback<"+returnType+">();\n");

                            codeBuilder.append(indent+ONE_INDENT+ONE_INDENT+"client_->sendRequest(address, "+interfaceId+", "+procId+",\n");

                            codeBuilder.append(indent+ONE_INDENT+ONE_INDENT+ONE_INDENT+" creds, auth, request, data, data_length, "+returnTypeBuilder+",\n");
                            codeBuilder.append(indent+ONE_INDENT+ONE_INDENT+ONE_INDENT+" NULL, sync_cb);\n");
                            codeBuilder.append(indent+ONE_INDENT+ONE_INDENT+"return sync_cb;\n");
                            codeBuilder.append(indent+ONE_INDENT+"}\n\n");

                            /*codeBuilder.append("    public RPCResponse<"+returnType+"> " + method.getName() + "(");
                            codeBuilder.append("InetSocketAddress server, Auth authHeader, UserCredentials userCreds, "+inputType+" input");
                            if (data_in)
                                codeBuilder.append(", ReusableBuffer data");
                            codeBuilder.append(") throws IOException {\n");
                            codeBuilder.append("         if (server == null) server = defaultServer;\n");
                            codeBuilder.append("         if (server == null) throw new IllegalArgumentException(\"defaultServer must be set in constructor if you want to pass null as server in calls\");\n");
                            codeBuilder.append("         RPCResponse<"+returnType+"> response = new RPCResponse<"+returnType+">("+returnType+".getDefaultInstance());\n");
                            codeBuilder.append("         client.sendRequest(server, authHeader, userCreds, "+interfaceId+", "+procId+", input, "+dataValue+", response, false);\n");
                            codeBuilder.append("         return response;\n");
                            codeBuilder.append("    }\n\n");

                            String[] unrolled = unrollInputMessage(proto, method.getInputType(), typeDefs);
                            codeBuilder.append("    public RPCResponse<"+returnType+"> " + method.getName() + "(");
                            codeBuilder.append("InetSocketAddress server, Auth authHeader, UserCredentials userCreds");
                            if (unrolled[0].length() > 0) {
                                codeBuilder.append(", ");
                                codeBuilder.append(unrolled[0]);
                            }
                            if (data_in)
                                codeBuilder.append(", ReusableBuffer data");
                            codeBuilder.append(") throws IOException {\n");
                            codeBuilder.append("         "+unrolled[1]+"\n");
                            codeBuilder.append("         return ");
                            codeBuilder.append(method.getName());
                            codeBuilder.append("(server, authHeader, userCreds, msg");
                            if (data_in)
                                codeBuilder.append(", data");
                            codeBuilder.append(");\n");
                            codeBuilder.append("    }\n\n");
                             * 
                             */
                        }


                        // codeBuilder.append("    public static void " +
                        // msg.getName() + "() {\n");
                        //
                        // for (FieldDescriptorProto field : msg.getFieldList())
                        // {
                        //
                        // String type = field.getTypeName();
                        // if(type.startsWith("."))
                        // type = type.substring(1);
                        //
                        // field.getType();
                        //
                        // codeBuilder.append("        public " + type + " " +
                        // field.getName() + ";\n");
                        // }
                        //

                        codeBuilder.append(indent+"private:\n");
                        codeBuilder.append(indent+ONE_INDENT+"Client* client_;\n");

                        codeBuilder.append(indent+"};\n");

                        for (int i = 0; i < namespaceTokens.length; i++) {
                            indent = indent.substring(ONE_INDENT.length());
                            codeBuilder.append(indent);
                            codeBuilder.append("}\n");
                        }

                        codeBuilder.append("#endif //"+className.toUpperCase()+"_H\n");

                        String file = codeBuilder.toString();

                        String extraIncludes = "";
                        for (String incl : includes) {
                            extraIncludes += "#include \""+incl+"\"\n";
                        }
                        file = file.replace("@@@INCLUDE@@@", extraIncludes);

                        //filePrefix+"/"+className + ".cpp"
                        File f = File.newBuilder().setName(proto.getName().replace(".proto", "ServiceClient.h")).setContent(
                            file).build();
                        responseBuilder.addFile(f);
                    }

                }

            }
        }

        responseBuilder.build().writeTo(System.out);

    }

    private static String[] unrollInputMessage(FileDescriptorProto file, String type, Map<String,TypeDef> typeDefs) {

        TypeDef unrollType = typeDefs.get(type);
        if (unrollType == null) {
            System.err.println("could not find message '"+type+"' to unroll");
            System.exit(1);
        }
        DescriptorProto message = unrollType.message;
        

        StringBuilder list = new StringBuilder();

        StringBuilder builder = new StringBuilder("final "+unrollType.fullName+" msg = "+unrollType.fullName+".newBuilder().");
        boolean wasEmpty = false;
        for (int i = 0; i < message.getFieldCount(); i++) {
            wasEmpty = false;
            final FieldDescriptorProto field = message.getField(i);
            final String FName = field.getName().substring(0, 1).toUpperCase()+field.getName().substring(1);
            switch (field.getType()) {
                case TYPE_MESSAGE:
                case TYPE_ENUM: {

                    TypeDef fieldType = typeDefs.get(field.getTypeName());
                    if (fieldType == null) {
                        System.err.println("could not find message '"+field.getTypeName()+"' for field "+field.getName());
                        System.exit(1);
                    }

                    if (field.getLabel() == FieldDescriptorProto.Label.LABEL_REPEATED) {
                        list.append("List<"+fieldType.fullName+"> "+field.getName());
                        builder.append("addAll"+FName+"("+field.getName()+")");
                    } else {
                        if (!field.getTypeName().contains("emptyRequest")) {
                            list.append(fieldType.fullName+" "+field.getName());
                            builder.append("set"+FName+"("+field.getName()+")");
                        } else {
                            System.out.println("empty field: "+field.getName());
                            wasEmpty = true;
                        }
                        
                    }
                    break;
                }
                default: {
                    if (field.getLabel() == FieldDescriptorProto.Label.LABEL_REPEATED) {
                        list.append(tttr.get(field.getType())+" "+field.getName());
                        builder.append("addAll"+FName+"("+field.getName()+")");
                    } else {
                        list.append(ttt.get(field.getType())+" "+field.getName());
                        builder.append("set"+FName+"("+field.getName()+")");
                    }
                    break;
                }
            }
            builder.append(".");
            if ((i < message.getFieldCount()-1) && !wasEmpty) {
                list.append(", ");
            }

            if ((i == message.getFieldCount()-1) && wasEmpty) {
                list.delete(list.length()-2,list.length());
            }


        }
        if (message.getFieldCount() > 0)
            builder.append("build();");
        else
            builder = new StringBuilder("final "+unrollType.fullName+" msg = "+unrollType.fullName+".getDefaultInstance();");

        return new String[]{list.toString(),builder.toString()};

    }

    private static void printMessage(List<DescriptorProto> msgs) {
        for (DescriptorProto msg : msgs) {
            System.err.println("message type: "+msg.getName());
            for (FieldDescriptorProto field : msg.getFieldList()) {
                switch (field.getType()) {
                    case TYPE_MESSAGE: {
                        System.err.println("     "+field.getName()+": "+field.getTypeName()+", "+field.getLabel().toString());
                        printMessage(msg.getNestedTypeList());
                        break;
                    }
                    case TYPE_ENUM: {
                        System.err.println("     "+field.getName()+": "+field.getTypeName());
                        break;
                    }
                    case TYPE_GROUP: {
                        System.err.println("     "+field.getName()+": List<"+field.getTypeName()+">");
                        break;
                    }
                    default: {
                        System.err.println("     "+field.getName()+": "+ttt.get(field.getType())+","+field.getLabel().toString());
                        break;
                    }
                }

            }
        }
    }

    private static class TypeDef {
        DescriptorProto message;
        String          fullName;
        String          fileName;
    }
}
