import React from 'react'

import DataBucket from 'components/DataBucket'

const DataGrouping = (props) => {
    if (props.data && props.data.size > 0) {
        const entryArray = [...props.data.entries()]
        const groups = entryArray.map(([key,value]) => (
            <tbody key={key}>
                <tr>
                    <th colSpan="8" className="rvt-ts-20 tableSubHeadOverride">{key}</th>
                </tr>
                <DataBucket data={value} updateCourseInState={props.updateCourseInState}/>
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
