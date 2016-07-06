package org.xtreemfs.pbrpcgen;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.xtreemfs.foundation.pbrpc.generatedinterfaces.PBRPC;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.File;

public class RPCSourceGenerator {

    private static final Map<Type,String> ttt;

    private static final Map<Type,String> tttr;

    static {
        ttt = new HashMap();
        ttt.put(Type.TYPE_BOOL,"boolean");
        ttt.put(Type.TYPE_BYTES,"ByteString");
        ttt.put(Type.TYPE_DOUBLE,"double");
        ttt.put(Type.TYPE_FIXED32,"int");
        ttt.put(Type.TYPE_FIXED64,"long");
        ttt.put(Type.TYPE_UINT32,"int");
        ttt.put(Type.TYPE_UINT64,"long");
        ttt.put(Type.TYPE_FLOAT,"float");
        ttt.put(Type.TYPE_STRING,"String");

        tttr = new HashMap();
        tttr.put(Type.TYPE_BOOL,"List<Boolean>");
        tttr.put(Type.TYPE_BYTES,"List<ByteString>");
        tttr.put(Type.TYPE_DOUBLE,"List<Double>");
        tttr.put(Type.TYPE_FIXED32,"List<Integer>");
        tttr.put(Type.TYPE_FIXED64,"List<Long>");
        tttr.put(Type.TYPE_UINT32,"List<Integer>");
        tttr.put(Type.TYPE_UINT64,"List<Long>");
        tttr.put(Type.TYPE_FLOAT,"List<Float>");
        tttr.put(Type.TYPE_STRING,"List<String>");
    }
    private static final String EMPTY_REQUEST = "emptyRequest";
    private static final String EMPTY_RESPONSE = "emptyResponse";

    //public static final String PACKAGE = "org.xtreemfs.foundation.pbrpc.generated";

    public static void main(String[] args) throws Exception {

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
                    final String fileName = filchen.getName().replace(".proto", "");
                    String pkgName = proto.getPackage();
                    if (pkgName.length() > 0)
                        pkgName = pkgName+".";
                    for (DescriptorProto msg : proto.getMessageTypeList()) {
                        TypeDef def = new TypeDef();
                        def.fullName = fileName+"."+msg.getName();
                        def.message = msg;
                        def.fileName = fileName;
                        typeDefs.put("."+pkgName+msg.getName(),def);

                        for (DescriptorProto nested : msg.getNestedTypeList()) {
                            TypeDef defN = new TypeDef();
                            defN.fullName = fileName + "." + msg.getName() + "." + nested.getName();
                            defN.message = nested;
                            defN.fileName = fileName;
                            typeDefs.put("." + pkgName + msg.getName() + "." + nested.getName(), defN);
                        }
                    }
                    for (EnumDescriptorProto msg : proto.getEnumTypeList()) {
                        TypeDef def = new TypeDef();
                        def.fullName = fileName+"."+msg.getName();
                        def.message = null;
                        def.fileName = fileName;
                        typeDefs.put("."+pkgName+msg.getName(),def);
                    }
                }
            }
        }
        System.err.println("typedefs: "+typeDefs.keySet());

        for (Entry<FieldDescriptor, Object> files : map.entrySet()) {

            if (files.getKey().getName().equals("proto_file")) {

                for (FileDescriptorProto proto : (List<FileDescriptorProto>) files.getValue()) {

                    final String javaPackage = proto.getOptions().getJavaPackage();
                    final String filePrefix = javaPackage.replace(".", "/");
                    java.io.File filchen = new java.io.File(proto.getName());
                    final String msgName = filchen.getName().replace(".proto", "");


                    //printMessage(proto.getMessageTypeList());

                    for (ServiceDescriptorProto srv : proto.getServiceList()) {
                        // proto.getName() returns the file name of the .proto file
                        // e.g. "xtreemfs/DIR.proto"
                        // Example: "xtreemfs/DIR.proto" -> "DIRServiceClient"
                        String className =  (new java.io.File(proto.getName())).getName().replace(".proto", "ServiceClient");
                        String classNameConst = (new java.io.File(proto.getName())).getName().replace(".proto", "ServiceConstants");
                        

                        StringBuilder codeBuilder = new StringBuilder();
                        StringBuilder codeBuilderConst = new StringBuilder();

                        codeBuilder.append("//automatically generated from "+filchen.getName()+" at "+new Date()+"\n");
                        codeBuilder.append("//(c) "+((new Date()).getYear()+1900)+". See LICENSE file for details.\n\n");
                        codeBuilder.append("package " + javaPackage + ";\n\n");

                        codeBuilderConst.append("//automatically generated from "+filchen.getName()+" at "+new Date()+"\n");
                        codeBuilderConst.append("//(c) "+((new Date()).getYear()+1900)+". See LICENSE file for details.\n\n");
                        codeBuilderConst.append("package " + javaPackage + ";\n\n");

                        codeBuilderConst.append("import com.google.protobuf.Message;\n");

                        codeBuilderConst.append("\n");
                        codeBuilderConst.append("public class " + classNameConst + " {\n\n");
                        
                        //imports
                        codeBuilder.append("import java.io.IOException;\n");
                        codeBuilder.append("import java.util.List;\n");
                        codeBuilder.append("import java.net.InetSocketAddress;\n");
                        codeBuilder.append("import com.google.protobuf.Message;\n");
                        codeBuilder.append("import com.google.protobuf.ByteString;\n");
                        codeBuilder.append("import org.xtreemfs.foundation.buffer.ReusableBuffer;\n");
                        codeBuilder.append("import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC;\n");
                        codeBuilder.append("import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;\n");
                        codeBuilder.append("import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;\n");
                        codeBuilder.append("import org.xtreemfs.foundation.pbrpc.client.RPCNIOSocketClient;\n");
                        codeBuilder.append("import org.xtreemfs.foundation.pbrpc.client.RPCResponse;\n");

                        codeBuilder.append("\n");
                        codeBuilder.append("public class " + className + " {\n\n");

                        //member declarations
                        codeBuilder.append("    private RPCNIOSocketClient client;\n");
                        codeBuilder.append("    private InetSocketAddress  defaultServer;\n");

                        codeBuilder.append("\n");
                        //methods

                        codeBuilder.append("    public "+className+"(RPCNIOSocketClient client, InetSocketAddress defaultServer) {\n");
                        codeBuilder.append("        this.client = client;\n");
                        codeBuilder.append("        this.defaultServer = defaultServer;\n");
                        codeBuilder.append("    }\n\n");


                        int interfaceId = srv.getOptions().getExtension(PBRPC.interfaceId);

                        codeBuilderConst.append("    public static final int INTERFACE_ID = " + interfaceId + ";\n");

                        StringBuilder cbRequest = new StringBuilder("\n    public static Message getRequestMessage(int procId) {\n");
                        cbRequest.append("        switch (procId) {\n");

                        StringBuilder cbResponse = new StringBuilder("\n    public static Message getResponseMessage(int procId) {\n");
                        cbResponse.append("        switch (procId) {\n");


                        for (MethodDescriptorProto method: srv.getMethodList()) {

                            System.err.println("input type: "+method.getInputType());
                            final String inputType = typeDefs.get(method.getInputType()).fullName;//msgName+method.getInputType();

                            final String returnType = typeDefs.get(method.getOutputType()).fullName;//msgName+method.getOutputType();

                            final int procId = method.getOptions().getExtension(PBRPC.procId);

                            final boolean isEmptyResponse = returnType.contains(EMPTY_RESPONSE);
                            final boolean isEmptyRequest = inputType.contains(EMPTY_REQUEST);

                            codeBuilderConst.append("    public static final int PROC_ID_"+method.getName().toUpperCase()+" = " + procId + ";\n");
                            if (isEmptyResponse)
                                cbResponse.append("           case "+procId+": return null;\n");
                            else
                                cbResponse.append("           case "+procId+": return "+returnType+".getDefaultInstance();\n");

                            if (isEmptyRequest)
                                cbRequest.append("           case "+procId+": return null;\n");
                            else
                                cbRequest.append("           case "+procId+": return "+inputType+".getDefaultInstance();\n");

                            final boolean data_in = method.getOptions().hasExtension(PBRPC.dataIn) ? method.getOptions().getExtension(PBRPC.dataIn) : false;
                            final String dataValue = data_in ? "data" : "null";

                            if (isEmptyResponse) {
                                codeBuilder.append("    public RPCResponse " + method.getName() + "(");
                            } else {
                                codeBuilder.append("    public RPCResponse<"+returnType+"> " + method.getName() + "(");
                            }
                            codeBuilder.append("InetSocketAddress server, Auth authHeader, UserCredentials userCreds, "+inputType+" input");
                            if (data_in)
                                codeBuilder.append(", ReusableBuffer data");
                            codeBuilder.append(") throws IOException {\n");
                            codeBuilder.append("         if (server == null) server = defaultServer;\n");
                            codeBuilder.append("         if (server == null) throw new IllegalArgumentException(\"defaultServer must be set in constructor if you want to pass null as server in calls\");\n");
                            if (!isEmptyResponse) {
                                codeBuilder.append("         RPCResponse<"+returnType+"> response = new RPCResponse<"+returnType+">("+returnType+".getDefaultInstance());\n");
                            } else {
                                codeBuilder.append("         RPCResponse response = new RPCResponse(null);\n");
                            }
                            codeBuilder.append("         client.sendRequest(server, authHeader, userCreds, "+interfaceId+", "+procId+", input, "+dataValue+", response, false);\n");
                            codeBuilder.append("         return response;\n");
                            codeBuilder.append("    }\n\n");

                            String[] unrolled = unrollInputMessage(proto, method.getInputType(), typeDefs);

                            if (isEmptyResponse) {
                                codeBuilder.append("    public RPCResponse " + method.getName() + "(");
                            } else {
                                codeBuilder.append("    public RPCResponse<"+returnType+"> " + method.getName() + "(");
                            }
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
                            codeBuilder.append("(server, authHeader, userCreds,");
                            if (isEmptyRequest) {
                                codeBuilder.append("null");
                            } else {
                                codeBuilder.append("msg");
                            }
                            if (data_in)
                                codeBuilder.append(", data");
                            codeBuilder.append(");\n");
                            codeBuilder.append("    }\n\n");
                        }

                        codeBuilder.append("    public boolean clientIsAlive() {\n");
                        codeBuilder.append("        return client.isAlive();\n");
                        codeBuilder.append("    }\n");

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

                        codeBuilder.append("}");

                        cbResponse.append("           default: throw new RuntimeException(\"unknown procedure id\");\n");
                        cbRequest.append("           default: throw new RuntimeException(\"unknown procedure id\");\n");
                        cbRequest.append("        }\n    }\n\n");
                        cbResponse.append("        }\n    }\n\n");

                        codeBuilderConst.append(cbRequest);
                        codeBuilderConst.append(cbResponse);
                        codeBuilderConst.append("\n}");

                        File f = File.newBuilder().setName(filePrefix+"/"+className + ".java").setContent(
                            codeBuilder.toString()).build();
                        responseBuilder.addFile(f);

                        f = File.newBuilder().setName(filePrefix+"/"+classNameConst + ".java").setContent(
                            codeBuilderConst.toString()).build();
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
                        builder.append("addAll"+camelCase(FName)+"("+field.getName()+")");
                    } else {
                        if (!field.getTypeName().contains("emptyRequest")) {
                            list.append(fieldType.fullName+" "+field.getName());
                            builder.append("set"+camelCase(FName)+"("+field.getName()+")");
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
                        builder.append("addAll"+camelCase(FName)+"("+field.getName()+")");
                    } else {
                        list.append(ttt.get(field.getType())+" "+field.getName());
                        builder.append("set"+camelCase(FName)+"("+field.getName()+")");
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
        else {
            if (unrollType.fullName.contains(EMPTY_REQUEST)) {
                builder = new StringBuilder("");
            } else {
                builder = new StringBuilder("final "+unrollType.fullName+" msg = "+unrollType.fullName+".getDefaultInstance();");
            }
        }
            

        return new String[]{list.toString(),builder.toString()};

    }

    private static String camelCase(String name) {
        int pos = name.indexOf('_');
        while ((pos >= 0) && (pos+2 <= name.length())) {
            String tmp = name.substring(0,pos);
            tmp += name.substring(pos+1, pos+2).toUpperCase();
            tmp += name.substring(pos+2);
            name = tmp;
            pos = name.indexOf('_');
        }
        return name;
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
