import React from 'react'

import TableRow from 'components/TableRow'

const DataBucket = (props) => {
    if (props.data.length > 0) {
        const courses = props.data.map((courseModel) => (
            <TableRow key={`${courseModel.course.id}-${courseModel.enrollment.role}`} courseModel={courseModel}
                updateCourseInState={props.updateCourseInState} groupByHeader={props.groupByHeader} />
        ))

        return ( <React.Fragment>{courses}</React.Fragment> );
    } else {
        return null;
    }
}

export default DataBucket
