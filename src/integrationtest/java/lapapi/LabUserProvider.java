//----------------------------------------------------------------------
//
// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
//
//------------------------------------------------------------------------------

package lapapi;

import org.testng.util.Strings;
import java.util.HashMap;
import java.util.Map;

public class LabUserProvider {
    private final KeyVaultSecretsProvider keyVaultSecretsProvider;
    private final LabService labService;
    private Map<UserQuery, LabResponse> userCache;

    public LabUserProvider(){
        keyVaultSecretsProvider = new KeyVaultSecretsProvider();
        labService = new LabService();
        userCache = new HashMap<>();
    }

    public LabResponse getDefaultUser() {
        UserQuery query =  new UserQuery.Builder().
                isMamUser(false).
                isMfaUser(false).
                isFederatedUser(false).
                build();
        return getLabUser(query);
    }

    public LabResponse getAdfsUser(FederationProvider federationProvider, boolean federated){
        UserQuery query = new UserQuery.Builder().
                isMamUser(false).
                isMfaUser(false).
                isFederatedUser(federated).
                federationProvider(federationProvider).
                build();
        return getLabUser(query);
    }

    public LabResponse getB2cUser(B2CIdentityProvider b2CIdentityProvider) {
        UserQuery query = new UserQuery.Builder().
                userType(UserType.B2C).
                b2CIdentityProvider(b2CIdentityProvider).
                build();
        return getLabUser(query);
    }

    private LabResponse getLabUser(UserQuery userQuery){
        if(userCache.containsKey(userQuery)){
            return userCache.get(userQuery);
        }
        LabResponse response = labService.getLabResponse(userQuery);
        userCache.put(userQuery, response);
        return response;
    }

    public String getUserPassword(LabUser user){
        if(!Strings.isNullOrEmpty(user.getPassword())){
            return user.getPassword();
        }
        if(Strings.isNullOrEmpty(user.getCredentialUrl())){
            throw new IllegalArgumentException("LabUser credential URL cannot be null");
        }
        try{
            String password = keyVaultSecretsProvider.getSecret(user.getCredentialUrl());
            user.setPassword(password);
            return password;
        } catch (Exception e){
            throw new RuntimeException("Error getting LabUser password: " + e.getMessage());
        }
    }
}
