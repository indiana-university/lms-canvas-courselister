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
        axios.post("app/" + action + "/" + courseId)
                .then(response => response.data)
                .then((data) => {
                    this.setState({hidden: data.hidden})
                    this.props.updateCourseInState(courseId, {value: data.hidden}, null)
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
        axios.post("app/" + action + "/" + courseId)
                .then(response => response.data)
                .then((data) => {
                    this.setState({favorited: data.favorited})
                    this.props.updateCourseInState(courseId, null, {value: data.favorited})
                })
    }

    render() {
        const courseModel = this.props.courseModel

        return (
            <tr>
                <td scope="row">
                    <FavoriteStatus enrollmentClassification={courseModel.enrollmentClassification} isFavorite={this.state.favorited}
                        isFavoritable={courseModel.favoritable} handleFavoriteCourse={this.handleFavoriteCourse}
                        handleUnfavoriteCourse={this.handleUnfavoriteCourse} courseName={courseModel.courseName} />
                </td>
                <td>
                    <HiddenStatus handleShowCourse={this.handleShowCourse} handleHideCourse={this.handleHideCourse}
                        isHidden={this.state.hidden} courseName={courseModel.courseName} />
                </td>
                <td className="searchable">
                    <CourseName courseName={courseModel.courseName} renderUrl={courseModel.linkClickable}
                        courseId={courseModel.course.id} /> <PendingBadge enrollmentStatus={courseModel.enrollment.enrollment_state} />
                </td>
                <td className="searchable">{courseModel.courseCode}</td>
                <td className="searchable">{courseModel.courseNickName}</td>
                <td>{courseModel.roleLabel}</td>
                <td>{courseModel.term.name}</td>
                <td>{courseModel.published ? 'Yes': 'No'}</td>
            </tr>
        );
    }
}

function HiddenStatus(props) {
    const isHidden = props.isHidden;
    if (isHidden) {
        const showText = "Show course: " + props.courseName;
        return <a href="#" aria-label={showText} onClick={props.handleShowCourse} className="no-decoration"><i className="fa fa-eye-slash courseHidden" title="Click to show the course in the list."></i></a>;
    }
    const hideText = "Hide course: " + props.courseName;
    return <a href="#" aria-label={hideText} onClick={props.handleHideCourse} className="no-decoration"><i className="fa fa-eye" title="Click to make the course hidden in the list."></i></a>;
}

function FavoriteStatus(props) {
    const isFavorite = props.isFavorite;
    const enrollmentClassification = props.enrollmentClassification;

    if (props.isFavoritable) {
        if (isFavorite) {
            const unfavorite = "Unfavorite: " + props.courseName;
            return <a href="#" aria-label={unfavorite} onClick={props.handleUnfavoriteCourse} className="no-decoration"><RvtSvg title="Click to remove from the Courses menu." icon="rvt-icon-star-solid"
                className="rvt-color-yellow" /></a>;
        }
        const favorite = "Favorite: " + props.courseName;
        return <a href="#" aria-label={favorite} onClick={props.handleFavoriteCourse} className="no-decoration"><RvtSvg title="Click to add to the Courses menu." icon="rvt-icon-star"/></a>;
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
