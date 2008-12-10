/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.
This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
Grid Operating System, see <http://www.xtreemos.eu> for more details.
The XtreemOS project has been developed with the financial support of the
European Commission's IST program under contract #FP6-033576.
XtreemFS is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free
Software Foundation, either version 2 of the License, or (at your option)
any later version.
XtreemFS is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.
You should have received a copy of the GNU General Public License
along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * AUTHORS: Bjoern Kolbeck (ZIB)
 */
package org.xtreemfs.common.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import org.xtreemfs.foundation.json.JSONParser;
import org.xtreemfs.foundation.json.JSONString;
import org.xtreemfs.foundation.pinky.channels.ChannelIO;
import org.xtreemfs.common.auth.AuthenticationException;
import org.xtreemfs.common.auth.AuthenticationProvider;
import org.xtreemfs.common.auth.UserCredentials;

/**
 * A simple provider that parses the JSON string sent in the authentication header
 * as described in the protocol spec.
 * @author bjko
 */
public class BackwdCompatNullAuthProvider implements AuthenticationProvider {

    public BackwdCompatNullAuthProvider() {

    }

    public UserCredentials getEffectiveCredentials(String authHeader, ChannelIO channel)
            throws AuthenticationException {

        if (authHeader.startsWith("{")) {
            //new JSON header format

            String GUID = null;
            List<String> GGIDs = null;
            String mech = null;
            try {
                //parse the JSON string in header field
                JSONString authStr = new JSONString(authHeader);
                Map<String, Object> authInfo = (Map<String, Object>) JSONParser.parseJSON(authStr);
                mech = (String) authInfo.get("mechanism");
                GUID = (String) authInfo.get("guid");
                GGIDs = (List<String>) authInfo.get("ggids");
            } catch (Exception ex) {
                throw new AuthenticationException("malformed authentication credentials: " + ex);
            }

            if (!mech.equals("nullauth")) {
                throw new AuthenticationException("unknown authorization mechanism: " + mech);
            }

            return new UserCredentials(GUID, GGIDs, GUID.equals("root"));
        } else {
            String GUID = null;
            List<String> GGIDs = null;
            //old header format for comapatability!
            StringTokenizer st = new StringTokenizer(
                    authHeader, " ");
            String mech = st.nextToken();

            if (mech.equals("nullauth")) {

                if (!st.hasMoreTokens()) {
                    throw new AuthenticationException("nullauth: user ID required");
                }

                // set the user ID
                GUID = st.nextToken();

                if (!st.hasMoreTokens()) {
                    throw new AuthenticationException("nullauth: at least one group ID required");
                }

                // set the group IDs
                GGIDs = new ArrayList<String>();
                while (st.hasMoreTokens()) {
                   GGIDs.add(st.nextToken());
                }

                return new UserCredentials(GUID, GGIDs, GUID.equals("root"));
            } else {
                throw new AuthenticationException("unknown authorization mechanism: " + mech);
            }

        }
    }

    public void initialize(boolean useSSL) throws RuntimeException {
    }
}
