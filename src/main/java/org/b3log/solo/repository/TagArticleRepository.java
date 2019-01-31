/*
 * Solo - A small and beautiful blogging system written in Java.
 * Copyright (c) 2010-2019, b3log.org & hacpai.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.b3log.solo.repository;

import org.b3log.latke.Keys;
import org.b3log.latke.ioc.BeanManager;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.repository.*;
import org.b3log.latke.repository.annotation.Repository;
import org.b3log.solo.model.Article;
import org.b3log.solo.model.Tag;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Tag-Article repository.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.6, Jan 28, 2019
 * @since 0.3.1
 */
@Repository
public class TagArticleRepository extends AbstractRepository {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(TagArticleRepository.class);

    /**
     * Public constructor.
     */
    public TagArticleRepository() {
        super(Tag.TAG + "_" + Article.ARTICLE);
    }

    /**
     * Gets most used tags with the specified number.
     *
     * @param num the specified number
     * @return a list of most used tags, returns an empty list if not found
     * @throws RepositoryException repository exception
     */
    public List<JSONObject> getMostUsedTags(final int num) throws RepositoryException {
        final List<JSONObject> records = select("SELECT\n" +
                "\t`tag_oId`,\n" +
                "\tcount(*) AS cnt\n" +
                "FROM `" + getName() + "`\n" +
                "GROUP BY\n" +
                "\t`tag_oId`\n" +
                "ORDER BY\n" +
                "\tcnt DESC\n" +
                "LIMIT ?", num);
        final List<JSONObject> ret = new ArrayList<>();
        final TagRepository tagRepository = BeanManager.getInstance().getReference(TagRepository.class);
        for (final JSONObject record : records) {
            final String tagId = record.optString(Tag.TAG + "_" + Keys.OBJECT_ID);
            final JSONObject tag = tagRepository.get(tagId);
            if (null != tag) {
                final int articleCount = getArticleCount(tagId);
                tag.put(Tag.TAG_T_PUBLISHED_REFERENCE_COUNT, articleCount);
            }
            ret.add(tag);
        }

        return ret;
    }

    /**
     * Gets article count of a tag specified by the given tag id.
     *
     * @param tagId the given tag id
     * @return article count, returns {@code -1} if occurred an exception
     */
    public int getArticleCount(final String tagId) {
        final Query query = new Query().setFilter(new PropertyFilter(Tag.TAG + "_" + Keys.OBJECT_ID, FilterOperator.EQUAL, tagId));
        try {
            return (int) count(query);
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Gets tag [" + tagId + "]'s article count failed", e);

            return -1;
        }
    }

    /**
     * Gets tag-article relations by the specified article id.
     *
     * @param articleId the specified article id
     * @return for example
     * <pre>
     * [{
     *         "oId": "",
     *         "tag_oId": "",
     *         "article_oId": articleId
     * }, ....], returns an empty list if not found
     * </pre>
     * @throws RepositoryException repository exception
     */
    public List<JSONObject> getByArticleId(final String articleId) throws RepositoryException {
        final Query query = new Query().setFilter(new PropertyFilter(Article.ARTICLE + "_" + Keys.OBJECT_ID, FilterOperator.EQUAL, articleId)).
                setPageCount(1);

        return getList(query);
    }

    /**
     * Gets tag-article relations by the specified tag id.
     *
     * @param tagId          the specified tag id
     * @param currentPageNum the specified current page number, MUST greater
     *                       then {@code 0}
     * @param pageSize       the specified page size(count of a page contains objects),
     *                       MUST greater then {@code 0}
     * @return for example
     * <pre>
     * {
     *     "pagination": {
     *       "paginationPageCount": 88250
     *     },
     *     "rslts": [{
     *         "oId": "",
     *         "tag_oId": tagId,
     *         "article_oId": ""
     *     }, ....]
     * }
     * </pre>
     * @throws RepositoryException repository exception
     */
    public JSONObject getByTagId(final String tagId, final int currentPageNum, final int pageSize) throws RepositoryException {
        final Query query = new Query().setFilter(new PropertyFilter(Tag.TAG + "_" + Keys.OBJECT_ID, FilterOperator.EQUAL, tagId)).
                addSort(Article.ARTICLE + "_" + Keys.OBJECT_ID, SortDirection.DESCENDING).
                setPage(currentPageNum, pageSize);

        return get(query);
    }
}
