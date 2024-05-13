/*-
 * #%L
 * lms-lti-courselist
 * %%
 * Copyright (C) 2015 - 2022 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
import React from 'react'
import axios from 'axios'

import RvtSvg from 'components/RvtSvg'

class TableRow extends React.Component {

    constructor(props) {
        super(props);

        this.state = {hidden: props.courseModel.hidden, favorited: props.courseModel.favorited}

        this.handleHideCourse.bind(this)
        this.handleShowCourse.bind(this)
        this.handleFavoriteCourse.bind(this)
        this.handleUnfavoriteCourse.bind(this)
    }

    handleHideCourse = (event) => {
        this.handleShowHideCourse("hide")
    }

    handleShowCourse = (event) => {
        this.handleShowHideCourse("show")
    }

    handleShowHideCourse = (action) => {
        const courseId = this.props.courseModel.course.id
        const courseName = this.props.courseModel.courseName;
        const hideText = action === "hide" ? courseName + ' was hidden in your course list.' : courseName + ' is now included in your course list.';

        // Add a loading spinner since the action to show/hide can take some time
        var loader = document.getElementById("visibility-loader-" + courseId);
        var visibilityBtn = document.getElementById("visibility-" + courseId);
        if (loader && visibilityBtn) {
            visibilityBtn.classList.add("rvt-display-none");
            loader.classList.remove("rvt-display-none");
        }

        axios.post("app/" + action + "/" + courseId)
                .then(response => response.data)
                .then((data) => {
                    this.setState({hidden: data.hidden})
                    this.props.updateCourseInState(courseId, {value: data.hidden}, null)
                    document.getElementById("srMessaging").innerHTML = hideText;

                    if (visibilityBtn && loader) {
                        visibilityBtn.classList.remove("rvt-display-none");
                        loader.classList.add("rvt-display-none");
                    }
                })
    }

    handleFavoriteCourse = (event) => {
        this.handleFavUnFavCourse("favorite")
    };

    handleUnfavoriteCourse = (event) => {
        this.handleFavUnFavCourse("unfavorite")
    };

    handleFavUnFavCourse = (action) => {
        const courseId = this.props.courseModel.course.id
        const courseName = this.props.courseModel.courseName;
        const favText = action === "favorite" ?  courseName + ' was added to the Courses menu.' : courseName + ' was removed from the Courses menu.';

        // add a loading spinner since the favoriting call can be slow
        var loader = document.getElementById("fav-loader-" + courseId);
        var favBtn = document.getElementById("fav-" + courseId);
        if (loader && favBtn) {
            favBtn.classList.add("rvt-display-none");
            loader.classList.remove("rvt-display-none");
        }

        axios.post("app/" + action + "/" + courseId)
                .then(response => response.data)
                .then((data) => {
                    this.setState({favorited: data.favorited})
                    this.props.updateCourseInState(courseId, null, {value: data.favorited})
                    document.getElementById("srMessaging").innerHTML = favText;

                    if (favBtn && loader) {
                        favBtn.classList.remove("rvt-display-none");
                        loader.classList.add("rvt-display-none");
                    }
                })
    }

    render() {
        const courseModel = this.props.courseModel
        const courseHeaderId = 'id-' + courseModel.course.id + '-' + _.kebabCase(courseModel.term.name) + '-' + _.kebabCase(courseModel.enrollment.enrollment_state) + '-' + _.kebabCase(courseModel.roleLabel)

        return (
            <tr>
                <th id={courseHeaderId} className="searchable" headers={`${this.props.groupByHeader} courseName`}>
                    <CourseName courseName={courseModel.courseName} renderUrl={courseModel.linkClickable}
                        courseId={courseModel.course.id} /> <PendingBadge enrollmentStatus={courseModel.enrollment.enrollment_state} />
                </th>
                <td headers={`${this.props.groupByHeader} ${courseHeaderId} fav`}>
                    <FavoriteStatus enrollmentClassification={courseModel.enrollmentClassification} isFavorite={this.state.favorited}
                        isFavoritable={courseModel.favoritable} handleFavoriteCourse={this.handleFavoriteCourse}
                        handleUnfavoriteCourse={this.handleUnfavoriteCourse} courseName={courseModel.courseName} courseId={courseModel.course.id} />
                </td>
                <td headers={`${this.props.groupByHeader} ${courseHeaderId} vis`}>
                    <HiddenStatus handleShowCourse={this.handleShowCourse} handleHideCourse={this.handleHideCourse}
                        isHidden={this.state.hidden} courseName={courseModel.courseName} courseId={courseModel.course.id} />
                </td>
                <td headers={`${this.props.groupByHeader} ${courseHeaderId} courseCode`} className="searchable">{courseModel.courseCode}</td>
                <td headers={`${this.props.groupByHeader} ${courseHeaderId} nickname`} className="searchable">{courseModel.courseNickName}</td>
                <td headers={`${this.props.groupByHeader} ${courseHeaderId} role`}>{courseModel.roleLabel}</td>
                <td headers={`${this.props.groupByHeader} ${courseHeaderId} term`}>{courseModel.term.name}</td>
                <td headers={`${this.props.groupByHeader} ${courseHeaderId} pub`}>{courseModel.published ? 'Yes': 'No'}</td>
            </tr>
        );
    }
}

function HiddenStatus(props) {
    const isHidden = props.isHidden;
    if (isHidden) {
        return <React.Fragment>
                    <button id={`visibility-${props.courseId}`} onClick={props.handleShowCourse} className="rvt-button rvt-button--plain" aria-describedby={`hide-status-${props.courseId}`}>
                        <RvtSvg aria-hidden="true" title="Click to show this course in the list." icon="rvt-icon-eye-off" />
                        <span className="rvt-sr-only">Show {props.courseName} in your course list.</span>
                    </button>
                    <span id={`visibility-loader-${props.courseId}`} className="rvt-loader rvt-loader--xxs rvt-display-none rvt-m-left-sm loading-spinner"></span>
                    <p id={`hide-status-${props.courseId}`} hidden>This course is currently hidden in your course list.</p>
                </React.Fragment>;
    }

    return <React.Fragment>
               <button id={`visibility-${props.courseId}`} onClick={props.handleHideCourse} className="rvt-button rvt-button--plain" aria-describedby={`hide-status-${props.courseId}`}>
                 <RvtSvg aria-hidden="true" title="Click to hide this course in the list." icon="rvt-icon-eye" />
                 <span className="rvt-sr-only">Hide {props.courseName} in your course list.</span>
               </button>
               <span id={`visibility-loader-${props.courseId}`} className="rvt-loader rvt-loader--xxs rvt-display-none rvt-m-left-sm loading-spinner"></span>
               <p id={`hide-status-${props.courseId}`} hidden>This course is currently included in your course list.</p>
           </React.Fragment>;
}

function FavoriteStatus(props) {
    const isFavorite = props.isFavorite;
    const enrollmentClassification = props.enrollmentClassification;

    if (props.isFavoritable) {
        if (isFavorite) {
            return <React.Fragment>
                        <button id={`fav-${props.courseId}`} onClick={props.handleUnfavoriteCourse} className="rvt-button rvt-button--plain" aria-describedby={`fav-status-${props.courseId}`}>
                            <RvtSvg aria-hidden="true" title="Click to remove from the Courses menu." icon="rvt-icon-star-solid"
                                className="rvt-color-yellow" />
                            <span className="rvt-sr-only">Remove {props.courseName} from the Courses menu.</span>
                        </button>
                        <span id={`fav-loader-${props.courseId}`} className="rvt-loader rvt-loader--xxs rvt-display-none rvt-m-left-sm loading-spinner"></span>
                        <p id={`fav-status-${props.courseId}`} hidden>This course is currently set as a favorite.</p>
                   </React.Fragment>;
        }

        return <React.Fragment>
                    <button id={`fav-${props.courseId}`} onClick={props.handleFavoriteCourse} className="rvt-button rvt-button--plain" aria-describedby={`fav-status-${props.courseId}`}>
                        <RvtSvg aria-hidden="true" title="Click to add to the Courses menu." icon="rvt-icon-star"/>
                        <span className="rvt-sr-only">Add {props.courseName} to the Courses menu.</span>
                    </button>
                    <span id={`fav-loader-${props.courseId}`} className="rvt-loader rvt-loader--xxs rvt-display-none rvt-m-left-sm loading-spinner"></span>
                    <p id={`fav-status-${props.courseId}`} hidden>This course is not currently a favorite.</p>
                </React.Fragment>;
    }
    return null;
}

function CourseName(props) {
    const courseUrl = canvasBaseUrl + "/courses/" + props.courseId;
    if (props.renderUrl) {
        return <a href={courseUrl} target="_parent">{props.courseName}</a>;
    } else {
        return <React.Fragment>{props.courseName}</React.Fragment>;
    }
}

function PendingBadge(props) {
    const enrollmentStatus = props.enrollmentStatus;
    if (enrollmentStatus=="pending" || enrollmentStatus=="invited") {
        return <span className="rvt-badge rvt-badge--info">Pending</span>;
    }
    return null;
}

export default TableRow
