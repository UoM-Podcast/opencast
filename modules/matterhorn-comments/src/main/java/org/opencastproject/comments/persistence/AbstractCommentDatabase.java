/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package org.opencastproject.comments.persistence;

import org.opencastproject.comments.Comment;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

public abstract class AbstractCommentDatabase implements CommentDatabase {

  private static final Logger logger = LoggerFactory.getLogger(AbstractCommentDatabase.class);

  public abstract EntityManagerFactory getEmf();

  @Override
  public void deleteComment(long commentId) throws CommentDatabaseException, NotFoundException {
    EntityManager em = getEmf().createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      Option<CommentDto> dto = CommentDatabaseUtils.find(Option.some(commentId), em, CommentDto.class);
      if (dto.isNone()) {
        throw new NotFoundException("Comment with ID " + commentId + " does not exist");
      }

      CommentDatabaseUtils.deleteReplies(dto.get().getReplies(), em);

      dto.get().setReplies(new ArrayList<CommentReplyDto>());
      em.remove(dto.get());
      tx.commit();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not delete comment: {}", ExceptionUtils.getStackTrace(e));
      if (tx.isActive()) {
        tx.rollback();
      }

      throw new CommentDatabaseException(e);
    } finally {
      if (em != null) {
        em.close();
      }
    }
  }

  @Override
  public CommentDto storeComment(Comment comment) throws CommentDatabaseException {
    EntityManager em = getEmf().createEntityManager();
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      CommentDto mergedComment = CommentDatabaseUtils.mergeComment(comment, em);
      tx.commit();
      return mergedComment;
    } catch (Exception e) {
      logger.error("Could not update or store comment: {}", ExceptionUtils.getStackTrace(e));
      if (tx.isActive()) {
        tx.rollback();
      }

      throw new CommentDatabaseException(e);
    } finally {
      if (em != null) {
        em.close();
      }
    }
  }

  @Override
  public CommentDto getComment(long commentId) throws NotFoundException, CommentDatabaseException {
    EntityManager em = getEmf().createEntityManager();
    try {
      Option<CommentDto> dto = CommentDatabaseUtils.find(Option.some(commentId), em, CommentDto.class);
      if (dto.isNone()) {
        throw new NotFoundException("No comment with id=" + commentId + " exists");
      }

      return dto.get();
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Could not retreive comment: {}", ExceptionUtils.getStackTrace(e));
      throw new CommentDatabaseException(e);
    } finally {
      if (em != null) {
        em.close();
      }
    }
  }

}
