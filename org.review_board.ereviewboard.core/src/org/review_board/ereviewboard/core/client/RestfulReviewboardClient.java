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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.Policy;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.review_board.ereviewboard.core.ReviewboardAttributeMapper;
import org.review_board.ereviewboard.core.ReviewboardCorePlugin;
import org.review_board.ereviewboard.core.exception.ReviewboardException;
import org.review_board.ereviewboard.core.model.Diff;
import org.review_board.ereviewboard.core.model.DiffComment;
import org.review_board.ereviewboard.core.model.Repository;
import org.review_board.ereviewboard.core.model.Review;
import org.review_board.ereviewboard.core.model.ReviewGroup;
import org.review_board.ereviewboard.core.model.ReviewReply;
import org.review_board.ereviewboard.core.model.ReviewRequest;
import org.review_board.ereviewboard.core.model.ReviewRequestStatus;
import org.review_board.ereviewboard.core.model.Screenshot;
import org.review_board.ereviewboard.core.model.ScreenshotComment;
import org.review_board.ereviewboard.core.model.ServerInfo;
import org.review_board.ereviewboard.core.model.User;

/**
 * RESTful implementation of {@link ReviewboardClient}.
 * 
 * @author Markus Knittig
 */
public class RestfulReviewboardClient implements ReviewboardClient {
    
    private static final int PAGED_RESULT_INCREMENT = 50;

    private final RestfulReviewboardReader reviewboardReader;

    private ReviewboardClientData clientData;

    private ReviewboardHttpClient httpClient;

    public RestfulReviewboardClient(AbstractWebLocation location, ReviewboardClientData clientData,
            TaskRepository repository) {
        this.clientData = clientData;

        reviewboardReader = new RestfulReviewboardReader();

        httpClient = new ReviewboardHttpClient(location, repository.getCharacterEncoding(),
                Boolean.valueOf(repository.getProperty("selfSignedSSL")));

        refreshRepositorySettings(repository);
    }

    public ReviewboardClientData getClientData() {
        return clientData;
    }

    public void refreshRepositorySettings(TaskRepository repository) {
        // Nothing to do yet
    }

    public List<Review> getReviews(final int reviewRequestId, IProgressMonitor monitor) throws ReviewboardException {
        
        PagedLoader<Review> loader = new PagedLoader<Review>(PAGED_RESULT_INCREMENT, monitor, "Retrieving reviews") {
            @Override
            protected PagedResult<Review> doLoadInternal(int start, int maxResults, IProgressMonitor monitor)
                    throws ReviewboardException {
                
                StringBuilder query = new StringBuilder();
                query.append("/api/review-requests/").append(reviewRequestId).append("/reviews/");
                query.append("?start=").append(start).append("&max-results=").append(maxResults);
               
                return reviewboardReader.readReviews(httpClient.executeGet(query.toString(), monitor));
            }
        };
        
        return loader.doLoad();
    }
    
    public List<ReviewReply> getReviewReplies(final int reviewRequestId, final int reviewId, IProgressMonitor monitor) throws ReviewboardException {
 
        PagedLoader<ReviewReply> loader = new PagedLoader<ReviewReply>(PAGED_RESULT_INCREMENT, monitor, "Retrieving review replies") {
            
            @Override
            protected PagedResult<ReviewReply> doLoadInternal(int start, int maxResults,
                    IProgressMonitor monitor) throws ReviewboardException {
                
                StringBuilder query = new StringBuilder();

                query.append("/api/review-requests/").append(reviewRequestId).append("/reviews/");
                query.append(reviewId).append("/replies/?start=").append(start).append("&max-results=" + maxResults);
                
                return reviewboardReader.readReviewReplies(httpClient.executeGet(query.toString(), monitor));
            }
        };
        
        return loader.doLoad();
    }
    
    public int countDiffCommentsForReply(int reviewRequestId, int reviewId, int reviewReplyId, IProgressMonitor reviewDiffMonitor) throws ReviewboardException {
        
        String result = httpClient.executeGet("/api/review-requests/" + reviewRequestId + "/reviews/"+reviewId+"/replies/" + reviewReplyId+"/diff-comments?counts-only=1", reviewDiffMonitor);
        
        return reviewboardReader.readCount(result);
    }
    
    public int countScreenshotCommentsForReply(int reviewRequestId, int reviewId, int reviewReplyId, IProgressMonitor reviewDiffMonitor) throws ReviewboardException {
        
        String result = httpClient.executeGet("/api/review-requests/" + reviewRequestId + "/reviews/"+reviewId+"/replies/" + reviewReplyId+"/screenshot-comments?counts-only=1", reviewDiffMonitor);
        
        return reviewboardReader.readCount(result);
    }

    public List<DiffComment> readDiffComments(final int reviewRequestId, final int reviewId, IProgressMonitor monitor) throws ReviewboardException {
        
        PagedLoader<DiffComment> loader = new PagedLoader<DiffComment>(PAGED_RESULT_INCREMENT, monitor, "Retrieving reviews") {
            @Override
            protected PagedResult<DiffComment> doLoadInternal(int start, int maxResults, IProgressMonitor monitor)
                    throws ReviewboardException {
                
                StringBuilder query = new StringBuilder();
                query.append("/api/review-requests/").append(reviewRequestId).append("/reviews/").append(reviewId);
                query.append("/diff-comments/").append("?start=").append(start).append("&max=results=").append(maxResults);
                query.append("?start=").append(start).append("&max-results=").append(maxResults);
               
                return reviewboardReader.readDiffComments(httpClient.executeGet(query.toString(), monitor));
            }
        };
        
        return loader.doLoad();
        
       
    }
    
    public int countDiffComments(int reviewRequestId, int reviewId, IProgressMonitor monitor) throws ReviewboardException {
        
        return reviewboardReader.readCount(httpClient.executeGet("/api/review-requests/" + reviewRequestId+"/reviews/" + reviewId +"/diff-comments?counts-only=1", monitor));
    }

    private List<Repository> getRepositories(IProgressMonitor monitor) throws ReviewboardException {
        
        PagedLoader<Repository> loader = new PagedLoader<Repository>(PAGED_RESULT_INCREMENT, monitor, "Retrieving repositories") {
            
            @Override
            protected PagedResult<Repository> doLoadInternal(int start, int maxResults, IProgressMonitor monitor) throws ReviewboardException {
                
                StringBuilder query = new StringBuilder();

                query.append("/api/repositories?start=").append(start).append("&max-results=" + maxResults);

                return reviewboardReader.readRepositories(httpClient.executeGet(query.toString(),
                            monitor));
            }
        };
        
        return loader.doLoad();
    }

    private List<User> getUsers(IProgressMonitor monitor) throws ReviewboardException {
    
        PagedLoader<User> loader = new PagedLoader<User>(PAGED_RESULT_INCREMENT, monitor, "Retrieving users") {

            @Override
            protected PagedResult<User> doLoadInternal(int start, int maxResults, IProgressMonitor monitor) throws ReviewboardException {
                
                StringBuilder query = new StringBuilder();
                
                query.append("/api/users?start=").append(start).append("&max-results="+maxResults);
                
                return reviewboardReader.readUsers(httpClient.executeGet(query.toString(), monitor));
            }
            
        };
        
        return loader.doLoad();
    }
    
    private List<ReviewGroup> getReviewGroups(IProgressMonitor monitor) throws ReviewboardException {
        
        PagedLoader<ReviewGroup> loader = new PagedLoader<ReviewGroup>(PAGED_RESULT_INCREMENT, monitor, "Retrieving review groups") {
            
            @Override
            protected PagedResult<ReviewGroup> doLoadInternal(int start, int maxResults, IProgressMonitor monitor) throws ReviewboardException {
                
                StringBuilder query = new StringBuilder();

                query.append("/api/groups?start=").append(start).append("&max-results=" + maxResults);

                return reviewboardReader.readGroups(httpClient.executeGet(query.toString(),
                            monitor));
            }
        };
        
        return loader.doLoad();
    }
    
    private TimeZone getTimeZone(IProgressMonitor monitor) throws ReviewboardException {
        
        monitor.beginTask("Retrieving server information", 1);
        
        try {
            return reviewboardReader.readServerInfo(httpClient.executeGet("/api/info", monitor)).getTimeZone();
        } finally {
            monitor.done();
        }
    }

    public List<ReviewRequest> getReviewRequests(final String query, int queryMaxResults, IProgressMonitor monitor) throws ReviewboardException {

        
        PagedLoader<ReviewRequest> loader = new PagedLoader<ReviewRequest>(PAGED_RESULT_INCREMENT, monitor, "Loading review requests") {
            
            @Override
            protected PagedResult<ReviewRequest> doLoadInternal(int start, int maxResults,
                    IProgressMonitor monitor) throws ReviewboardException {
                
                StringBuilder stringBuilder = new StringBuilder();

                stringBuilder.append("/api/review-requests/");
                // TODO: move this to a better location
                stringBuilder.append(query.replaceFirst("&max-results=[\\d]+", ""));
                stringBuilder.append("&start=").append(start).append("&max-results=").append(maxResults);
                
                return reviewboardReader.readReviewRequests(httpClient.executeGet(stringBuilder.toString(), monitor));
            }
        };
        loader.setLimit(queryMaxResults);
        
        return loader.doLoad();
    }
    
    public List<Diff> loadDiffs(int reviewRequestId, IProgressMonitor monitor) throws ReviewboardException {
        
        return reviewboardReader.readDiffs(httpClient.executeGet("/api/review-requests/" + reviewRequestId+"/diffs", monitor));
    }

    public List<Screenshot> loadScreenshots(int reviewRequestId, IProgressMonitor monitor) throws ReviewboardException {
        
        return reviewboardReader.readScreenshots(httpClient.executeGet("/api/review-requests/" + reviewRequestId+"/screenshots", monitor));
    }
    
    public List<ScreenshotComment> getScreenshotComments(final int reviewRequestId, final int screenshotId, final IProgressMonitor screenshotCommentMonitor) throws ReviewboardException {
        
        PagedLoader<ScreenshotComment> loader = new PagedLoader<ScreenshotComment>(PAGED_RESULT_INCREMENT, screenshotCommentMonitor, "Retrieving screenshot comments") {
            
            @Override
            protected PagedResult<ScreenshotComment> doLoadInternal(int start, int maxResults, IProgressMonitor monitor) throws ReviewboardException {
                
                StringBuilder query = new StringBuilder();
                query.append("/api/review-requests/").append(reviewRequestId);
                query.append("/screenshots/").append(screenshotId).append("/screenshot-comments");
                query.append("/?start=").append(start).append("&max-results=" + maxResults);
                
                return reviewboardReader.readScreenshotComments(httpClient.executeGet(query.toString(), screenshotCommentMonitor));
            }
        };
        
        return loader.doLoad();
    }
    
    private List<Integer> getReviewRequestIds(String query, IProgressMonitor monitor)
            throws ReviewboardException {
        return reviewboardReader.readReviewRequestIds(
                httpClient.executeGet("/api/review-requests/" + query, monitor));
    }

    public boolean hasRepositoryData() {
        return (clientData.lastupdate != 0);
    }

    public void updateRepositoryData(boolean force, IProgressMonitor monitor) throws ReviewboardException {
        
        if (hasRepositoryData() && !force)
            return;
        
        monitor.beginTask("Refreshing repository data", 100);

        try {
            
            // users usually outnumber groups and repositories
            // try to get good progress reporting by approximating the ratios
            // repositories with small data sets will not need very accurate progress reporting anyway
            clientData.setUsers(getUsers(Policy.subMonitorFor(monitor, 90)));
            
            clientData.setGroups(getReviewGroups(Policy.subMonitorFor(monitor, 5)));

            clientData.setRepositories(getRepositories(Policy.subMonitorFor(monitor, 4)));
            
            clientData.setTimeZone(getTimeZone(Policy.subMonitorFor(monitor, 1)));

            clientData.lastupdate = new Date().getTime();
        } finally  {
            monitor.done();
        }
    }

    public byte[] getRawDiff(int reviewRequestId, int diffRevision, IProgressMonitor monitor) throws ReviewboardException {
        
        return httpClient.executeGetForBytes("/api/review-requests/" + reviewRequestId + "/diffs/" + diffRevision +"/","text/x-patch", monitor);
    }
    
    public byte[] getScreenshot(String url, IProgressMonitor monitor) throws ReviewboardException {
        return httpClient.executeGetForBytes("/" + url, "image/*", monitor);
    }

    public IStatus validate(String username, String password, IProgressMonitor monitor) {

        try {
            
            if ( !httpClient.apiEntryPointExist(monitor) )
                return new Status(IStatus.ERROR, ReviewboardCorePlugin.PLUGIN_ID, "Repository not found. Please make sure that the path to the repository correct and the  server version is at least 1.5");
            
            Policy.advance(monitor, 1);
            
            httpClient.login(username, password, monitor);
            
            Policy.advance(monitor, 1);
            
            ServerInfo serverInfo = reviewboardReader.readServerInfo(httpClient.executeGet("/api/info", monitor));
            
            Policy.advance(monitor, 1);
            
            if ( !serverInfo.isAtLeast(1, 5) )
                return new Status(IStatus.ERROR, ReviewboardCorePlugin.PLUGIN_ID, "The version " + serverInfo.getProductVersion() + " is not supported. Please use a repository version of 1.5 or newer.");
            
            return Status.OK_STATUS;
        } catch ( ReviewboardException e ) {
            return new Status(IStatus.ERROR, ReviewboardCorePlugin.PLUGIN_ID, e.getMessage(), e);
        } catch (Exception e) {
            return new Status(IStatus.ERROR, ReviewboardCorePlugin.PLUGIN_ID, "Unexpected exception : " + e.getMessage(), e);
        } finally {
            monitor.done();
        }
    }
    
    public List<Integer> getReviewsIdsChangedSince(Date timestamp, IProgressMonitor monitor) throws ReviewboardException {
        
        try {
            if ( timestamp == null )
                throw new IllegalArgumentException("Timestamp may not be null");
            
            String query = "?status=all&max-results=10000&last-updated-from=" + URLEncoder.encode( ReviewboardAttributeMapper.newIso86011DateFormat().format(timestamp), "UTF-8");
            return getReviewRequestIds( query, monitor);
            
        } catch (UnsupportedEncodingException e) {
            throw new ReviewboardException("Failed encoding the query url", e);
        }
        
    }

    public ReviewRequest getReviewRequest(int reviewRequestId, IProgressMonitor monitor) throws ReviewboardException {

        return reviewboardReader.readReviewRequest(httpClient.executeGet("/api/review-requests/" + reviewRequestId + "/", monitor));
    }

    public void updateStatus(int reviewRequestId, ReviewRequestStatus status, IProgressMonitor monitor) throws ReviewboardException {

        if ( status == ReviewRequestStatus.ALL || status == ReviewRequestStatus.NONE  || status == null)
            throw new ReviewboardException("Invalid status to update to : " + status );
        
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("status", status.asSubmittableValue());
        
        String result = httpClient.executePut("/api/review-requests/"+reviewRequestId+"/", parameters, monitor);
        
        reviewboardReader.ensureSuccess(result);
    }
}
