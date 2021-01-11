import React from 'react'

import DataBucket from 'components/DataBucket'

const DataGrouping = (props) => {
    if (props.data && props.data.size > 0) {
        const entryArray = [...props.data.entries()]
        const groups = entryArray.map(([key,value]) => (
            <tbody key={key}>
                <tr>
                    <th colSpan="8" scope="colGroup" id={`group_${key.replace(' ', '_')}`} className="rvt-ts-20 tableSubHeadOverride">{key}</th>
                </tr>
                <DataBucket data={value} groupByHeader={`group_${key.replace(' ', '_')}`} updateCourseInState={props.updateCourseInState}/>
            </tbody>
        ))

        return (
            <React.Fragment>
                {groups}
            </React.Fragment>
            );
    } else {
        return null;
    }
}

export default DataGrouping
