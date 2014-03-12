package org.xtreemfs.contrib.provisioning;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xtreemfs.common.libxtreemfs.Client;
import org.xtreemfs.common.libxtreemfs.Options;
import org.xtreemfs.common.libxtreemfs.Volume;
import org.xtreemfs.common.libxtreemfs.exceptions.AddressToUUIDNotFoundException;
import org.xtreemfs.common.libxtreemfs.exceptions.PosixErrorException;
import org.xtreemfs.common.libxtreemfs.exceptions.VolumeNotFoundException;
import org.xtreemfs.contrib.provisioning.JsonRPC.METHOD;
import org.xtreemfs.foundation.SSLOptions;
import org.xtreemfs.foundation.json.JSONException;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.Auth;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthPassword;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.AuthType;
import org.xtreemfs.foundation.pbrpc.generatedinterfaces.RPC.UserCredentials;
import org.xtreemfs.mrc.utils.MRCHelper.SysAttrs;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC;
import org.xtreemfs.pbrpc.generatedinterfaces.DIR.Service;
import org.xtreemfs.pbrpc.generatedinterfaces.GlobalTypes.KeyValuePair;
import org.xtreemfs.pbrpc.generatedinterfaces.MRC.XAttr;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2ParamsType;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;


public abstract class AbstractRequestHandler implements RequestHandler {

  String[] methodNames;
  Client client;

  public AbstractRequestHandler(Client c, METHOD[] methodNames) {
    this.client = c;
    this.methodNames = new String[methodNames.length];
    for (int i = 0; i < methodNames.length; i++) {
      this.methodNames[i] = methodNames[i].toString();
    }
  }

  /**
   * Reports the method names of the handled requests
   */
  public String[] handledRequests() {
    return this.methodNames;
  }


  /**
   * this method has to be implemented
   * @param req
   * @param ctx
   * @return
   */
  public abstract JSONRPC2Response doProcess(JSONRPC2Request req, MessageContext ctx) throws Exception;

  // Processes the requests
  @Override
  public JSONRPC2Response process(JSONRPC2Request req, MessageContext ctx) {
    try {
      return doProcess(req, ctx);
    } catch (AccessToNonExistantParameter e) {
      Logger.getLogger(JsonRPC.class.getName()).log(Level.WARNING, e.getMessage());
      return new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.PARSE_ERROR.getCode(), e.getMessage()), req.getID());
    } catch (NumberFormatException e) {
      Logger.getLogger(JsonRPC.class.getName()).log(Level.WARNING, e.getMessage());
      return new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INVALID_PARAMS.getCode(), e.getMessage()), req.getID());
    } catch (IOException e) {
      Logger.getLogger(JsonRPC.class.getName()).log(Level.WARNING, e.getMessage());
      return new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), e.getMessage()), req.getID());
    } catch (Exception e) {
      Logger.getLogger(JsonRPC.class.getName()).log(Level.WARNING, e.getMessage());
      return new JSONRPC2Response(new JSONRPC2Error(JSONRPC2Error.INTERNAL_ERROR.getCode(), e.getMessage()), req.getID());
    }

  }

  public class AccessToNonExistantParameter extends Exception {
    private static final long serialVersionUID = 5654583602506228694L;
    public AccessToNonExistantParameter(String msg) {
      super(msg);
    }
  }

  public Object getObjectParam(JSONRPC2Request req, String key,
      boolean nullAllowed) throws AccessToNonExistantParameter, JSONRPC2Error {
    return getInputParam(req, key, Object.class, nullAllowed, "");
  }

  public String getStringParam(JSONRPC2Request req, String key, boolean nullAllowed) throws AccessToNonExistantParameter, JSONRPC2Error {
    return getInputParam(req, key, String.class, nullAllowed, "");
  }

  public String getStringParam(JSONRPC2Request req, String key, boolean nullAllowed, String nullValue) throws AccessToNonExistantParameter, JSONRPC2Error {
    return getInputParam(req, key, String.class, nullAllowed, nullValue);
  }

  public Long getLongParam(JSONRPC2Request req, String key, boolean nullAllowed, Long nullValue) throws AccessToNonExistantParameter, JSONRPC2Error {
    return getInputParam(req, key, Long.class, nullAllowed, nullValue);
  }

  public Integer getIntParam(JSONRPC2Request req, String key, boolean nullAllowed, Integer nullValue) throws AccessToNonExistantParameter, JSONRPC2Error {
    return getInputParam(req, key, Long.class, nullAllowed, nullValue != null? new Long(nullValue) : null).intValue();
  }

  public Boolean getBooleanParam(JSONRPC2Request req, String key, boolean nullAllowed, Boolean nullValue) throws AccessToNonExistantParameter, JSONRPC2Error {
    return getInputParam(req, key, Boolean.class, nullAllowed, nullValue);
  }

  @SuppressWarnings({ "unchecked" })
  public <T> T getInputParam(JSONRPC2Request req, String key, final Class<T> clazz, boolean nullAllowed, T nullValue) throws AccessToNonExistantParameter, JSONRPC2Error {
    Object value = null;

    if (req.getParamsType() == JSONRPC2ParamsType.NO_PARAMS
        && !nullAllowed) {
      throw new AccessToNonExistantParameter("Param '" + key + "' not available.");
    }
    // positional parameters
    else if (req.getParamsType() == JSONRPC2ParamsType.ARRAY) {
      List<Object> params = (List<Object>) req.getParams();

      if (params.isEmpty() && nullAllowed) {
        return nullValue;
      }
      else if (params.isEmpty() && !nullAllowed) {
        throw new AccessToNonExistantParameter("Null value for '"+key+"' not allowed");
      }

      value = params.remove(0);
    }
    // parameters by name
    else if (req.getParamsType() == JSONRPC2ParamsType.OBJECT) {
      Map<String, Object> params = (Map<String, Object>) req.getParams();
      value = params.get(key);

      // check for null value
      if (!nullAllowed && value == null) {
        throw new AccessToNonExistantParameter("Null value for '"+key+"' not allowed");
      }

    }

    if (value != null && clazz.isAssignableFrom(value.getClass())) {
      return (T)value;
    }
    else if (value != null) {
      throw new JSONRPC2Error(
          JSONRPC2Error.INVALID_PARAMS.getCode(),
          "Invalid type for param '" + key + "': '" + clazz.getSimpleName() + "' expected.");
    }

    return nullValue;
  }

  public static String getParameter(String key, Service s) {
    for (KeyValuePair pair : s.getData().getDataList()) {
      // check if the OSD is alive (last seen within the last 300 seconds)
      if (pair.getKey().equals(key)) {
        return pair.getValue();
      }
    }
    return "";
  }

  public Volume openVolume(String volume_name, SSLOptions sslOptions)
      throws AddressToUUIDNotFoundException, VolumeNotFoundException,
      IOException {
    Options options = new Options();
    return this.client.openVolume(volume_name, sslOptions, options);
  }

  public static Integer parseIntegerSafe(Object value) {
    if (value instanceof String) {
      if (value == null || ((String)value).trim().equals("")) {
        return 0;
      }
      return Integer.valueOf((String)value);
    }
    else if (value instanceof Number) {
      return (Integer) value;
    }
    throw new NumberFormatException();
  }

  public static Auth getAuth(String password) {
    Auth auth = Auth.newBuilder().setAuthType(AuthType.AUTH_PASSWORD)
        .setAuthPasswd(AuthPassword.newBuilder().setPassword(password))
        .build();
    return auth;
  }


  public static UserCredentials getGroups() {
    List<String> groups = new LinkedList<String>();
    groups.add("groupname");
    final UserCredentials uc = UserCredentials.newBuilder()
        .setUsername("root").addAllGroups(groups).build();
    return uc;
  }

  public static List<String> getUsableOSDs(Volume volume, final UserCredentials uc)
      throws IOException, PosixErrorException,
      AddressToUUIDNotFoundException, JSONException {
    List<String> usableOsds = new ArrayList<String>();
    MRC.listxattrResponse stat = volume.listXAttrs(uc, "/");
    for (XAttr attr : stat.getXattrsList()) {
//      System.out.println(attr.getName() + " " + attr.getValue());
      if (attr.getName().contains(SysAttrs.usable_osds.name())) {
        Map<String, String> values = (Map<String, String>) JSONParser.parseJSON(new JSONString(attr.getValue()));
        if (values != null) {
          for (Entry<String, String> v : values.entrySet()) {
            usableOsds.add(v.getKey());
          }
        }
      }        
    }
    return usableOsds;
  }
}
