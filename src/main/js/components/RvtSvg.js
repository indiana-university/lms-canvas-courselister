import React from 'react'

import icons from 'rivet-icons/dist/rvt-icons.svg'

const RvtSvg = (props) => {
    var title = ""
    if (props.title) {
        title = <title>{props.title}</title>
    }

    var classes = ""
    if (props.className) {
        classes = props.className
    }

    return (
        <svg className={`rvt-icon ${classes}`} onClick={props.onClick}>
            <use href={`${rvtSvgBase}#${props.icon}`}>
                {title}
            </use>
        </svg>
    )
}

export default RvtSvg