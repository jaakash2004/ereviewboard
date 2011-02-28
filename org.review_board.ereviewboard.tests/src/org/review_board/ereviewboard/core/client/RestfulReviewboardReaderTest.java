/*******************************************************************************
 * Copyright (c) 2004 - 2009 Mylyn project committers and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Mylyn project committers, Atlassian, Sven Krzyzak
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2009 Markus Knittig
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * Contributors:
 *     Markus Knittig - adapted Trac, Redmine & Atlassian implementations for
 *                      Review Board
 *******************************************************************************/
package org.review_board.ereviewboard.core.client;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import junit.framework.TestCase;

import org.review_board.ereviewboard.core.model.*;

/**
 * @author Markus Knittig
 *
 */
public class RestfulReviewboardReaderTest extends TestCase {

    private RestfulReviewboardReader testReader;

    protected void setUp() throws Exception {
        super.setUp();
        testReader = new RestfulReviewboardReader();
    }

    private String inputStreamToString(InputStream in) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;

        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line + "\n");
        }
        bufferedReader.close();

        return stringBuilder.toString();
    }

    public void testReadUsers() throws Exception {
        
        // http://www.reviewboard.org/docs/manual/dev/webapi/2.0/resources/user-list/
        InputStream in = getClass().getResourceAsStream("/jsondata/users.json");

        List<User> users = testReader.readUsers(inputStreamToString(in));

        assertThat("users.size", users.size(), is(4));
        
        User user = users.get(0);
        
        assertThat("users[0].email", user.getEmail(), is("admin@example.com"));
        assertThat("users[0].firstName", user.getFirstName(), is("Admin"));
        assertThat("users[0].fullName", user.getFullName(), is("Admin User"));
        assertThat("users[0].id", user.getId(), is(1));
        assertThat("users[0].lastName", user.getLastName(), is("User"));
        assertThat("users[0].userName", user.getUsername(), is("admin"));
        assertThat("users[0].url", user.getUrl(), is("/users/admin/"));
    }

    public void testReadGroups() throws Exception {

        // http://www.reviewboard.org/docs/manual/dev/webapi/2.0/resources/review-group-list/
        InputStream in = getClass().getResourceAsStream("/jsondata/groups.json");

        List<ReviewGroup> groups = testReader.readGroups(inputStreamToString(in));

        assertThat("groups.size", groups.size(), is(4));
        
        ReviewGroup firstGroup = groups.get(0);
        assertThat("groups[0].displayName", firstGroup.getDisplayName(), is("Dev Group"));
        assertThat("groups[0].id", firstGroup.getId(), is(1));
        assertThat("groups[0].mailingList", firstGroup.getMailingList(), is("devgroup@example.com"));
        assertThat("groups[0].name", firstGroup.getName(), is("devgroup"));
        assertThat("groups[0].url", firstGroup.getUrl(), is("/groups/devgroup/"));
    }

    public void testReadRepositories() throws Exception {
        
        // http://www.reviewboard.org/docs/manual/dev/webapi/2.0/resources/repository-list/
        InputStream in = getClass().getResourceAsStream("/jsondata/repositories.json");

        List<Repository> repositories = testReader.readRepositories(inputStreamToString(in));

        assertThat("repositories.size", repositories.size(), is(2));
        
        Repository repository = repositories.get(0);
        assertThat("repositories[0].id", repository.getId(), is(1));
        assertThat("repositories[0].name", repository.getName(), is("Review Board SVN"));
        assertThat("repositories[0].path", repository.getPath(), is("http://reviewboard.googlecode.com/svn"));
        assertThat("repositories[0].tool", repository.getTool(), is("Subversion"));
    }

    public void testReadReviewRequests() throws Exception {
        InputStream in = getClass().getResourceAsStream("/jsondata/review_requests.json");

        List<ReviewRequest> reviewRequests = testReader.readReviewRequests(inputStreamToString(in));

        assertEquals(1, reviewRequests.size());
    }

    public void testReadReviewRequest() throws Exception {
        InputStream in = getClass().getResourceAsStream("/jsondata/review_request.json");

        ReviewRequest reviewRequest = testReader.readReviewRequest(inputStreamToString(in));

        assertNotNull(reviewRequest);
        assertEquals(2, reviewRequest.getTargetPeople().size());
    }

    public void testReadReviews() throws Exception {
        InputStream in = getClass().getResourceAsStream("/jsondata/reviews.json");

        List<Review> reviews = testReader.readReviews(inputStreamToString(in));

        assertNotNull(reviews);
        assertEquals(1, reviews.get(0).getId());
    }

    public void testReadReviewComments() throws Exception {
        InputStream in = getClass().getResourceAsStream("/jsondata/review_comments.json");

        List<Comment> comments = testReader.readComments(inputStreamToString(in));

        assertNotNull(comments);
        assertEquals(1, comments.get(0).getId());
    }

    public void testReadReviewReplies() throws Exception {
        InputStream in = getClass().getResourceAsStream("/jsondata/review_replies.json");

        List<Review> comments = testReader.readReplies(inputStreamToString(in));

        assertNotNull(comments);
        assertEquals(2, comments.get(0).getId());
    }

}
