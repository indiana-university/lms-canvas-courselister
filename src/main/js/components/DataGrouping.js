import React from 'react'

import DataBucket from 'components/DataBucket'
import kebabCase from 'lodash';

const DataGrouping = (props) => {
    if (props.data && props.data.size > 0) {
        const entryArray = [...props.data.entries()]
        const groups = entryArray.map(([key,value]) => (
            <tbody key={key}>
                <tr>
                    <th colSpan="8" scope="colGroup" id={`group-${_.kebabCase(key)}`} className="rvt-ts-20 tableSubHeadOverride">{key}</th>
                </tr>
                <DataBucket data={value} groupByHeader={`group-${_.kebabCase(key)}`} updateCourseInState={props.updateCourseInState}/>
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
