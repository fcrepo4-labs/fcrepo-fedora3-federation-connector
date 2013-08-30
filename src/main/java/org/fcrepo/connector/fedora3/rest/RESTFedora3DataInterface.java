/**
 * Copyright 2013 DuraSpace, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.connector.fedora3.rest;

import static org.slf4j.LoggerFactory.getLogger;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.fcrepo.connector.fedora3.Fedora3DataInterface;
import org.fcrepo.connector.fedora3.FedoraDatastreamRecord;
import org.fcrepo.connector.fedora3.FedoraObjectRecord;
import org.slf4j.Logger;

import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.FedoraClientException;
import com.yourmediashelf.fedora.client.FedoraCredentials;
import com.yourmediashelf.fedora.generated.access.DatastreamType;
import com.yourmediashelf.fedora.generated.access.ObjectProfile;

public class RESTFedora3DataInterface implements Fedora3DataInterface {

    private static final Logger LOGGER
        = getLogger(RESTFedora3DataInterface.class);

    private FedoraClient fc;

    /**
     * Constructor with credentials necessary for a connection to fedora's REST
     * API.
     */
    public RESTFedora3DataInterface(String fedoraUrl, String username,
            String password) throws MalformedURLException,
            FedoraClientException {
        fc = new FedoraClient(
                new FedoraCredentials(fedoraUrl, username, password));
        String ver = FedoraClient.describeRepository().execute(fc)
                .getRepositoryVersion();
        LOGGER.debug("Initialized connection to fedora "
                + ver + " at " + fedoraUrl + ".");
    }

    /**
     * {@inheritDoc}
     */
    public FedoraObjectRecord getObjectByPid(String pid) {
        try {
            DefaultFedoraObjectRecord r = new DefaultFedoraObjectRecord();
            r.pid = pid;
            ObjectProfile op = FedoraClient.getObjectProfile(pid)
                    .execute(fc).getObjectProfile();
            r.createdDate = op.getObjCreateDate()
                    .toGregorianCalendar().getTime();
            r.lastModDate = op.getObjLastModDate()
                    .toGregorianCalendar().getTime();
            List<String> datastreams = new ArrayList<String>();
            for (DatastreamType ds : FedoraClient.listDatastreams(pid)
                    .execute(fc).getDatastreams()) {
                datastreams.add(ds.getDsid());
            }
            r.datastreams = datastreams;
            return r;
        } catch (FedoraClientException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean doesObjectExist(String pid) {
        try {
            return FedoraClient.getObjectProfile(pid).execute(fc).getStatus()
                    == 200;
        } catch (FedoraClientException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String[] getObjectPids(int offset, int pageSize) {
        try {
            List<String> pids = FedoraClient.findObjects()
                    .maxResults(pageSize).query("").pid().execute(fc).getPids();
            String[] result = new String[pids.size()];
            for (int i = 0; i < pids.size(); i ++) {
                result[i] = pids.get(i);
            }
            LOGGER.info("At least " + pids.size() + " objects found");
            return result;
        } catch (FedoraClientException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    public FedoraDatastreamRecord getDatastream(String pid, String dsid) {
        try {
            return new RESTFedoraDatastreamRecord(
                    FedoraClient.getDatastream(pid, dsid).execute(fc)
                    .getDatastreamProfile());
        } catch (FedoraClientException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean doesDatastreamExist(String pid, String dsid) {
        try {
            return FedoraClient.getDatastream(pid, dsid).execute(fc)
                    .getStatus() == 200;
        } catch (FedoraClientException ex) {
            throw new RuntimeException(ex);
        }
    }

}
