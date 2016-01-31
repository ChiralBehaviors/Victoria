/**
 * Copyright (c) 2016 Chiral Behaviors, LLC, all rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.victoria.oddbabble;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.chiralbehaviors.CoRE.agency.Agency;
import com.chiralbehaviors.CoRE.meta.models.AbstractModelTest;
import com.chiralbehaviors.CoRE.meta.workspace.dsl.WorkspaceImporter;
import com.chiralbehaviors.victoria.agency.Interest;
import com.chiralbehaviors.victoria.agency.User;

/**
 * @author hparry
 * 
 *         A test class to show examples of using the Phantasm API.
 *
 */
public class ExampleTests extends AbstractModelTest {

    @Before
    public void loadWorkspace() throws Exception {
        //        Our data model. contains the concepts of User, Interest, etc
        //        There's a maven plugin that will take this file and generate
        //        java classes from it.
        //        Generated classes are in /target/generated-sources/phantasm
        //        
        //        We manifest it here to populate the postgres database with our
        //        data model. Once that's done, we can wrap db rows in Phantasm
        //        instances in memory. 
        WorkspaceImporter.manifest(this.getClass().getResourceAsStream("/victoria.wsp"), model);

        //        Another workspace, this time full of test data for our scenarios.
        //        The maven plugin is not configured to generate java classes from
        //        this workspace. 
        WorkspaceImporter.manifest(this.getClass().getResourceAsStream("/testData.wsp"), model);

        //        Mostly, AbstractModelTest handles transactions for Unit tests that
        //        extend it. At the beginning of a test case, a transaction is Active.
        //        It is rolled back at the end of the test case rather than committed. 
        //        It makes for faster tests. However, we need the imported workspace
        //        data to be committed and available for all tests so we explicitly
        //        commit it here.
        em.getTransaction().commit();
        em.getTransaction().begin();
    }

    @Test
    public void testUsers() throws Exception {
        //        To construct a User object from existing data, first find the
        //        db row that defines the user Agency. This object contains the
        //        pk of the base rule.
        Agency lydiaRule = model.find("Lydia", Agency.class);

        //        Then, wrap the db row object in a User object. This will fail
        //        if the db row object is not an Agency.
        User lydia = model.wrap(User.class, lydiaRule);

        //        Same thing as a one liner
        User aela = model.wrap(User.class, model.find("Aela", Agency.class));

        //        The User phantasm is really a shortcut for lots of sql queries
        //        across various network tables masquerading as a POJO. The shape
        //        of the User object is defined in the victoria.wsp workspace.
        assertEquals(0, lydia.getDistrustses().size());

        //        We can also create Users directly, rather than playing around with 
        //        workspace files.
        User astrid = model.construct(User.class, "Astrid", "Director of Brotherly Love");
        astrid.setFirstName("Astrid");
        astrid.setLastName("Soeur");
        astrid.setEmailAddress("test@example.com");
        astrid.setPhoneNumber("555-5555");
        astrid.addTrusts(aela);

        //        Astrid is an assassin, so she's more interested in daggers than swords and heavy 
        //        armor. We'll have to add a new interest. 
        Interest dagger = model.construct(Interest.class, "Dagger", "Silent but deadly");

        //        notice here that weapon is a product rather than an agency. 
        //        Weapon is defined in testData.wsp rather than victoria.wsp so 
        //        there are no generated phantasms for it. 
        Agency weapon = model.find("Weapon", Agency.class);

        //        We need to classify dagger as a type of weapon in the database
        model.getAgencyModel().link(dagger.getRuleform(), model.getKernel().getIsA(), weapon,
                                     model.getKernel().getCore());

        //        We also need to associate that interest with Astrid. 
        astrid.addInterest(dagger);

        //        Lydia doesn't really trust Astrid because of that whole assassin thing. We should reflect
        //        that in the db. Without phantasms, we'd do the following: 
        //        model.getAgencyModel().link(lydia.getRuleform(), model.find("Distrusts", Relationship.class),
        //                                    aela.getRuleform(), model.getKernel().getCore());
        lydia.addDistrusts(astrid);

        assertEquals(1, lydia.getDistrustses().size());
    }

}
