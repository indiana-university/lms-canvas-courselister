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
import styled from 'styled-components'
import axios from 'axios'

import DataGrouping from 'components/DataGrouping'
import Loading from 'components/Loading'
import RvtSvg from 'components/RvtSvg'
import FilterTermOptions from 'components/FilterTermOptions'
import {CircleArrow as ScrollUpButton} from 'react-scroll-up-button'

import { chain, groupBy, sortBy } from 'lodash';

import 'rivet-clearable-input/dist/css/rivet-clearable-input.min.css';
import ClearableInput from 'rivet-clearable-input/dist/js/rivet-clearable-input.js';
import Mark from 'mark.js/dist/mark.es6.min.js'

class App extends React.Component {
    /**
     * Initialization stuff
     */
    constructor() {
        super()

        // Set the x-auth-token head for all requests
        // The customId value got injected in to the react.html file and is a global variable
        axios.defaults.headers.common['X-Auth-Token'] = customId;
        axios.defaults.headers.common[csrfHeaderName] = csrfValue;
//        axios.interceptors.request.use(request => {
//            console.debug('Starting Request', request)
//            return request
//        })

        this.state = {
            courses: [],
            allTerms: [],
            activeTerms: [],
            loading: true,
            grouping: "enrollmentClassification.text",
            groupSort: {
                key: "enrollmentClassification.order",
                direction: "asc"
            },
            groupedCourses: new Map(),
            orderKey: {
                key: "courseName",
                direction: "asc"
            },
            searchTerms: "",
            filteredEnrollments: [],
            filteredVisibility: [false],
            filteredPublished: [],
            filteredTerms: [],
            showOnlyActiveTerms: true,
            moreTermsClick: false,
            fewerTermsClick: false
        }
        this.handleGroupByOptionChange.bind(this)
        this.handleOrdering.bind(this)
        this.handleSearch.bind(this)
        this.handleSearchKeyPress.bind(this)
        this.handleFilter.bind(this)
        this.handleFilterBatch.bind(this)
        this.updateStateBatch.bind(this)
        this.updateCourseInState.bind(this)
        this.groupByMenuSpecialHandling.bind(this)
        this.handleGroupByMenuOpening.bind(this)
    }

    /**
     * Call off to the REST endpoints to load data
     */
    componentDidMount() {
        var self = this;
        axios.all([getCourses()])
            .then(axios.spread(function (courses) {
                var availableEnrollments = _.chain(courses.data).orderBy("enrollmentClassification.order", "asc").map("enrollmentClassification.name").uniq().value();

                var allTerms = _.chain(courses.data).orderBy("termSort", "desc").map("term").uniqBy("id").value();
                var activeTerms = _.filter(allTerms, function(term) { return term.active; });

                //If there aren't any active terms, just use the top 5
                if (activeTerms.length == 0) {
                    activeTerms = _.take(allTerms, 5);
                }

                self.setState({
                    courses: courses.data,
                    loading: false,
                    filteredEnrollments: [],
                    allTerms: allTerms,
                    activeTerms: activeTerms,
                    filteredTerms: []
                });
            }))
            .catch(error => {
                alert(error);
            });

        //Initialize the clearable input
        ClearableInput.init()
        //Setup the listener for when the text is cleared
        window.addEventListener('inputCleared', this.handleSearch);
    }

    componentWillUnmount() {
        window.removeEventListener('inputCleared', this.handleSearch);
    }

    componentDidUpdate() {
        var context = document.querySelectorAll("td.searchable");
        var instance = new Mark(context);

        instance.unmark().mark(this.state.searchTerms);

        // If the user clicked "More terms", we need to manually refocus to the
        // first "additional" term shown
        if (this.state.moreTermsClick) {
            var numActiveTerms = this.state.activeTerms.length
            var nextTerm = document.querySelectorAll("input[name='termCheckboxes']")[numActiveTerms]
            if (nextTerm) {
                nextTerm.focus()
            }

            this.state.moreTermsClick = false
        }

        // If the user clicked "Less terms" we need to manually refocus to the
        // "More terms" link
        if (this.state.fewerTermsClick) {
            // when the user clicks "Less Terms" we refocus on the "More Terms" link
            var moreTermsLink = document.getElementById("showMoreTerms")
            if (moreTermsLink) {
                moreTermsLink.focus()
            }

            this.state.fewerTermsClick = false
        }
    }

    cloneObject(input) {
        return JSON.parse(JSON.stringify(input));
    }

    updateCourseInState = (courseId, hidden, favorited) => {
        var courses = this.cloneObject(this.state.courses);
        var course = _.find(courses, function(dc) { return dc.course.id == courseId; });

        if (hidden) {
            course.hidden = hidden.value;
        }

        if (favorited) {
            course.favorited = favorited.value;
        }

        this.setState({courses: courses})
    }

    /**
     * Render
     */
    render() {
        //Filter on search terms
        const queryTerm = this.state.searchTerms.toLowerCase();

        var filteredCourses = this.state.courses.filter((decoratedCourse) => {
            return (decoratedCourse.courseName.toLowerCase().includes(queryTerm) ||
                (decoratedCourse.courseNickName && decoratedCourse.courseNickName.toLowerCase().includes(queryTerm)) ||
                (decoratedCourse.courseCode && decoratedCourse.courseCode.toLowerCase().includes(queryTerm)))
        });

        // filtering for published and unpublished courses
        filteredCourses = filteredCourses.filter((decoratedCourse) => {
            // if no filters or all filters
            if (this.state.filteredPublished.length==0 || this.state.filteredPublished.length==2) {
                return true;
            } else {
                return decoratedCourse.published==this.state.filteredPublished[0];
            }
        });

        // filtering for hidden and visible courses
        filteredCourses = filteredCourses.filter((decoratedCourse) => {
            // if no filters or all filters
            if (this.state.filteredVisibility.length==0 || this.state.filteredVisibility.length==2) {
                return true;
            } else {
                return decoratedCourse.hidden==this.state.filteredVisibility[0];
            }
        });

        // filtering for enrollment types
        filteredCourses = filteredCourses.filter((decoratedCourse) => {
            var includedEnrollments;
            if (this.state.filteredEnrollments.length > 0) {
                includedEnrollments = [...this.state.filteredEnrollments]
            } else {
                // if no filters, include all enrollments
                includedEnrollments = _.chain(this.state.courses).orderBy("enrollmentClassification.order", "asc").map("enrollmentClassification.name").uniq().value();
            }
            return includedEnrollments.indexOf(decoratedCourse.enrollmentClassification.name) !== -1;
        });

        // filtering for terms
        filteredCourses = filteredCourses.filter((decoratedCourse) => {
            var includedTerms;
            if (this.state.filteredTerms.length > 0) {
                includedTerms = [...this.state.filteredTerms]
            } else {
                includedTerms =  _.map(this.state.allTerms, 'id')
            }

            return includedTerms.indexOf(decoratedCourse.term.id) !== -1;
        });

        var groupedCourses = groupAndSortBuckets(filteredCourses, this.state.grouping, this.state.groupSort, this.state.orderKey)

        var filters = {filteredEnrollments: this.state.filteredEnrollments, filteredPublished: this.state.filteredPublished,
            filteredVisibility: this.state.filteredVisibility, filteredTerms: this.state.filteredTerms}

        return (
            <div>
                <div className="rvt-container" id="main-container">
                    <Loading loading={this.state.loading} />
                    <Header loading={this.state.loading} />
                    <ActionBar loading={this.state.loading} selectedGroup={this.state.grouping} courses={this.state.courses}
                        allTerms={this.state.allTerms} activeTerms={this.state.activeTerms} filters={filters}
                        showOnlyActiveTerms={this.state.showOnlyActiveTerms}
                        handleShowMoreTermsClick={this.handleShowMoreTermsClick} updateStateBatch={this.updateStateBatch}
                        handleGroupByOptionChange={this.handleGroupByOptionChange} handleSearch={this.handleSearch}
                        handleSearchKeyPress={this.handleSearchKeyPress} handleFilter={this.handleFilter}
                        handleFilterBatch={this.handleFilterBatch} handleRemoveAllFilters={this.handleRemoveAllFilters} 
                        groupByMenuSpecialHandling={this.groupByMenuSpecialHandling}
                        handleGroupByMenuOpening={this.handleGroupByMenuOpening} />
                    <MainTable loading={this.state.loading} groupedCourses={groupedCourses} orderKey={this.state.orderKey}
                        handleOrdering={this.handleOrdering} updateCourseInState={this.updateCourseInState} selectedGroup={this.state.grouping}/>
                </div>
                <ScrollUpButton />
            </div>
        );
    }

    /**
     * Change the groupBy options
     */
    handleGroupByOptionChange = (event) => {
        var groupKey = event.target.value
        var sortKey = event.target.getAttribute("data-sort-key") || groupKey
        var sortDir = event.target.getAttribute("data-sort-dir") || 'asc'

        this.changeGroupOptions(groupKey, sortKey, sortDir)
    }
    
    changeGroupOptions = (groupKey, sortKey, sortDir) => {
        this.setState({grouping: groupKey, groupSort: {key: sortKey, direction: sortDir}});
    }

    /**
     * Change the ordering options
     */
    handleOrdering = (event) => {
        var currentSortValue = this.state.orderKey.key;
        var currentSortDirection = this.state.orderKey.direction;
        var newSortValue = event.target.getAttribute("new-sort-value");

        if (currentSortValue==newSortValue) {
            /* Just flip the direction */
            if (currentSortDirection=="asc") {
                this.setState({orderKey: {key: newSortValue, direction: "desc"}});
            } else {
                this.setState({orderKey: {key: newSortValue, direction: "asc"}});
            }
        } else {
            /* new type and default it to 'asc' */
            this.setState({orderKey: {key: newSortValue, direction: "asc"}});
        }
    };

    handleSearch = (event) => {
        this.setState({searchTerms: document.getElementById('search').value});
    }

    handleSearchKeyPress = (event) => {
        if (event.key == 'Enter') {
            this.handleSearch(event);
        }
    }
    
    groupByMenuSpecialHandling = (event) => {  
        // If it was a tab, we are moving out of the dropdown and need to close it 
        // This is due to a bug in rivet dropdown that assumes all inputs are tabbable. This is not
        // the case for radio buttons which are navigated via arrow keys. If you are focused on the
        // first or second radio button in the group, tabbing out of the menu will not close it. 
        if (event.keyCode == 9) {
            Dropdown.close("dropdown-grouping");
        }
        
        // Rivet added key handlers to force nav with up/down arrows. However, radio buttons already navigate with up/down
        // natively, so Rivet's handler is causing non-standard behavior for radio button navigation. Let's
        // just prevent Rivet from doing any up/down handling with radio buttons in Firefox. Chrome does not 
        // have this issue
        if(navigator.userAgent.indexOf("Firefox") != -1 ) {
            
            const UP = 38;
            const DOWN = 40;
            
            if (event.keyCode == UP || event.keyCode == DOWN) {
                // stop rivet's keyboard handling from happening and we will just handle it ourselves
                event.preventDefault();
                
                // we need to select the correct radio button. If we are at the top we have to select the
                // bottom and vice versa    
                var radioInputs = document.getElementsByName('group-options');
                var currSelection = event.target;
                var newSelectedIndex;

                for (var i=0; i < radioInputs.length; i++) {
                    if (currSelection.id === radioInputs[i].id) {
                        if (event.keyCode == UP) {
                            if (i == 0) {
                                newSelectedIndex = radioInputs.length-1;
                            } else {
                                newSelectedIndex = i-1;
                            }
                        } else if (event.keyCode == DOWN) {
                            if (i == radioInputs.length-1) {
                                newSelectedIndex = 0;
                            } else {
                                newSelectedIndex = i+1;
                            }
                        }
                        break;
                    } 
                }

                radioInputs[newSelectedIndex].checked = true;
                
                var groupKey = radioInputs[newSelectedIndex].value
                var sortKey = radioInputs[newSelectedIndex].getAttribute("data-sort-key") || groupKey
                var sortDir = radioInputs[newSelectedIndex].getAttribute("data-sort-dir") || 'asc'
        
                this.changeGroupOptions(groupKey, sortKey, sortDir)           
            }
        }
    }
    
    handleGroupByMenuOpening = (event) => {
        if(navigator.userAgent.indexOf("Firefox") != -1 && event.keyCode == 40) {
            // when we use the down arrow to expand the menu, the focus should
            // move to the first radio button
            event.preventDefault();
            var radioInputs = document.getElementsByName('group-options');
            radioInputs[0].focus(); 
        }
    }

    handleFilter = (event) => {
        var value = event.target.value;
        var checked = event.target.checked;

        const data = this.handleFilterBatch(value, checked);
        this.updateStateBatch(data);
    }

    handleFilterBatch = (value, checked, data) => {

        const clonedState = data ? this.cloneObject(data) : this.cloneObject(this.state);
        var filteredPublished = clonedState.filteredPublished;
        var filteredVisibility = clonedState.filteredVisibility;
        var filteredEnrollments = clonedState.filteredEnrollments;
        var filteredTerms = clonedState.filteredTerms;

        const stateArray = ["PENDING", "CURRENT", "FUTURE", "PAST"];

        if (checked==true) {
            /* Add data! */
            if (stateArray.includes(value)) {
                filteredEnrollments.splice(0, 0, value);
            } else if ("publishedCourses"==value) {
                filteredPublished.splice(0, 0, true);
            } else if ("unpublishedCourses"==value) {
                filteredPublished.splice(0, 0, false);
            } else if ("visibleCourses"==value) {
                filteredVisibility.splice(0, 0, false);
            } else if ("hiddenCourses"==value) {
                filteredVisibility.splice(0, 0, true);
            } else {
                filteredTerms.splice(0, 0, value);
            }
        } else {
            /* Remove data! */
            if (stateArray.includes(value)) {
                filteredEnrollments.splice(filteredEnrollments.indexOf(value), 1);
            } else if ("publishedCourses"==value) {
                filteredPublished.splice(filteredPublished.indexOf(true), 1);
            } else if ("unpublishedCourses"==value) {
                filteredPublished.splice(filteredPublished.indexOf(false), 1);
            } else if ("visibleCourses"==value) {
                filteredVisibility.splice(filteredVisibility.indexOf(false), 1);
            } else if ("hiddenCourses"==value) {
                filteredVisibility.splice(filteredVisibility.indexOf(true), 1);
            } else {
                filteredTerms.splice(filteredTerms.indexOf(value), 1);
            }
        }

        const obj = {filteredEnrollments: _.uniq(filteredEnrollments), filteredPublished: _.uniq(filteredPublished),
                                filteredVisibility: _.uniq(filteredVisibility), filteredTerms: _.uniq(filteredTerms)};

        return obj;
//        this.setState(obj);
    }

    updateStateBatch = (obj) => {
        this.setState(obj)
    }

    /**
     * De-select all filters
     */
    handleRemoveAllFilters = (event) => {
        const data = {filteredEnrollments: [],
                     filteredPublished: [],
                     filteredVisibility: [],
                     filteredTerms: []};
        this.updateStateBatch(data);

        // move keyboard focus to the first filter element
        const firstFilter = document.getElementsByClassName('filter-input')[0];
        if (firstFilter) {
            firstFilter.focus();
        }
    };

}

function getCourses() {
    return axios.get(`app/courses`);
}

function Header(props) {
    if (props.loading) {
        return null;
    } else {
        return (
        <div className="rvt-flex-md-up rvt-justify-space-between-md-up rvt-p-top-sm rvt-m-bottom-md">
            <div>
                <h1 className="rvt-ts-36">Canvas Course List</h1>
            </div>
            <div>
                <div className="rvt-button-group rvt-button-group--right">
                    <a className="rvt-button rvt-button--secondary" href={browseCoursesUrl} target="_parent">Browse More Courses</a>
                    <a className="rvt-button rvt-button--secondary" href={siteRequestUrl} target="_parent">Start a New Course</a>
                </div>
            </div>
        </div>
        );
    }
}

function ActionBar(props) {
    // When a user clicks this link, we need to use display none instead
    // of not rendering it at all because rivet will listen for the click
    // event and, if the element doesn't exist in the dropdown, it
    // closes the dropdown (which we dont want)
    var showRemoveLink = props.filters.filteredEnrollments.length > 0 ||
                         props.filters.filteredVisibility.length > 0 ||
                         props.filters.filteredPublished.length > 0 ||
                         props.filters.filteredTerms.length > 0;
    let removeFilters = (
        <a id="removeFilters" className={"rvt-link-bold showMoreTerms " + (showRemoveLink ? '' : 'rvt-display-none')}
            onClick={props.handleRemoveAllFilters} href="#">Remove All Filters</a>
    )
    

    if (props.loading) {
        return null;
    } else {
        return (
        <div className="rvt-flex-md-up rvt-row">

        <h2 className="sr-only">Options to filter, group, and search courses</h2>

        <div className="rvt-dropdown" role="region" aria-label="Filter controls">
            <button
                 type="button"
                className="rvt-button rvt-button--secondary rvt-m-right-sm-md-up"
                data-dropdown-toggle="dropdown-filters"
                aria-haspopup="true"
                aria-expanded="false"
            >
                <span className="dropdown__toggle-text">Filter By</span>
                <svg aria-hidden="true" role="img" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16"><path fill="currentColor" d="M8,12.46a2,2,0,0,1-1.52-.7L1.24,5.65a1,1,0,1,1,1.52-1.3L8,10.46l5.24-6.11a1,1,0,0,1,1.52,1.3L9.52,11.76A2,2,0,0,1,8,12.46Z"></path></svg>
            </button>
            <div className="rvt-dropdown__menu" id="dropdown-filters" aria-hidden="true">
                {removeFilters}
                <fieldset className="rvt-p-left-sm">
                    <legend className="rvt-text-bold">Enrollments</legend>
                    <EnrollmentOptions courses={props.courses} filteredEnrollments={props.filters.filteredEnrollments} handleFilter={props.handleFilter} />
                </fieldset>
                <fieldset className="rvt-m-top-sm rvt-p-left-sm">
                    <legend className="rvt-text-bold">Course Visibility</legend>
                    <ul className="rvt-plain-list">
                        <li>
                            <input type="checkbox" id="visibleCourses" name="hiddenStatusCheckboxes" className="filter-input"
                                    value="visibleCourses" onChange={props.handleFilter}
                                    checked={props.filters.filteredVisibility.includes(false)} />
                            <label htmlFor="visibleCourses" className="rvt-m-right-sm rvt-text-nobr">Visible (<i className="fa fa-eye" aria-hidden="true" title="Visible icon"></i>)</label>
                        </li>
                        <li>
                            <input type="checkbox" id="hiddenCourses" name="hiddenStatusCheckboxes" className="filter-input"
                                    value="hiddenCourses" onChange={props.handleFilter}
                                    checked={props.filters.filteredVisibility.includes(true)} />
                            <label htmlFor="hiddenCourses" className="rvt-m-right-sm rvt-text-nobr">Hidden (<i className="fa fa-eye-slash courseHidden" aria-hidden="true" title="Hidden icon"></i>)</label>
                        </li>
                    </ul>
                </fieldset>
                <fieldset className="rvt-m-top-sm rvt-p-left-sm">
                    <legend className="rvt-text-bold">Published</legend>
                    <ul className="rvt-plain-list">
                        <li>
                            <input type="checkbox" id="publishedCourses" name="publishedStatusCheckboxes" className="filter-input"
                                value="publishedCourses" onChange={props.handleFilter}
                                 checked={props.filters.filteredPublished.includes(true)} />
                            <label htmlFor="publishedCourses" className="rvt-m-right-sm rvt-text-nobr">Yes</label>
                        </li>
                        <li>
                            <input type="checkbox" id="unpublishedCourses" name="publishedStatusCheckboxes" className="filter-input"
                                value="unpublishedCourses" onChange={props.handleFilter}
                                 checked={props.filters.filteredPublished.includes(false)} />
                            <label htmlFor="unpublishedCourses" className="rvt-m-right-sm rvt-text-nobr">No</label>
                        </li>
                    </ul>
                </fieldset>
                <fieldset className="rvt-m-top-sm rvt-p-left-sm">
                    <legend className="rvt-text-bold">Terms</legend>
                    <FilterTermOptions activeTerms={props.activeTerms} allTerms={props.allTerms} filteredTerms={props.filters.filteredTerms}
                        handleFilterBatch={props.handleFilterBatch} updateStateBatch={props.updateStateBatch} showOnlyActiveTerms={props.showOnlyActiveTerms} />
                </fieldset>
            </div>
        </div>

        <div className="rvt-dropdown" role="region" aria-label="Controls for grouping courses">
            <button
                 type="button"
                className="rvt-button rvt-button--secondary rvt-m-right-sm-md-up"
                data-dropdown-toggle="dropdown-grouping"
                aria-haspopup="true"
                aria-expanded="false"
                onKeyDown={props.handleGroupByMenuOpening}
            >
                <span className="dropdown__toggle-text">Group By</span>
                <svg aria-hidden="true" role="img" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16"><path fill="currentColor" d="M8,12.46a2,2,0,0,1-1.52-.7L1.24,5.65a1,1,0,1,1,1.52-1.3L8,10.46l5.24-6.11a1,1,0,0,1,1.52,1.3L9.52,11.76A2,2,0,0,1,8,12.46Z"></path></svg>
            </button>
            <div className="rvt-dropdown__menu" id="dropdown-grouping" aria-hidden="true">
                <fieldset className="rvt-p-left-sm">
                    <legend className="sr-only">Grouping options</legend>
                    <ul className="rvt-plain-list">
                        <li>
                            <input type="radio" name="group-options" id="group-options-enrl" value="enrollmentClassification.text"
                                checked={"enrollmentClassification.text" === props.selectedGroup} onChange={props.handleGroupByOptionChange}
                                data-sort-key="enrollmentClassification.order"
                                onKeyDown={props.groupByMenuSpecialHandling} />
                            <label htmlFor="group-options-enrl" className="rvt-m-right-sm">Enrollments</label>
                        </li>
                        <li>
                            <input type="radio" name="group-options" id="group-options-term" value="term.name"
                                checked={"term.name" === props.selectedGroup} onChange={props.handleGroupByOptionChange}
                                data-sort-key="termSort" data-sort-dir="desc" 
                                onKeyDown={props.groupByMenuSpecialHandling} />
                            <label htmlFor="group-options-term">Term</label>
                        </li>
                        <li>
                            <input type="radio" name="group-options" id="group-options-role" value="baseRoleLabel"
                                checked={"baseRoleLabel" === props.selectedGroup} onChange={props.handleGroupByOptionChange} 
                                onKeyDown={props.groupByMenuSpecialHandling} />
                            <label htmlFor="group-options-role">Role</label>
                        </li>
                    </ul>
                </fieldset>
            </div>
        </div>



            <label htmlFor="search" className="rvt-sr-only">Search Courses</label>
            <div className="rvt-input-group rvt-m-bottom-md">
                <div className="rvt-clearable-input-group search-input">
                    <input className="rvt-input-group__input rvt-clearable-input search-input" type="text" id="search"
                        onKeyPress={props.handleSearchKeyPress} />
                </div>
                <div className="rvt-input-group__append">
                    <button className="rvt-button" onClick={props.handleSearch}>Search courses</button>
                </div>
            </div>
        </div>
        );
    }
}

function MainTable(props) {
    if (props.loading) {
        return null;
    } else if (props.groupedCourses.size == 0) {
        return (<p className="rvt-m-bottom-md rvt-ts-32 rvt-text-center">No results</p>)
    } else {
        var srOnlyHeading = "Table of courses";
        if( "enrollmentClassification.text" === props.selectedGroup) {
            srOnlyHeading += " grouped by enrollment"
        } else if ("term.name" === props.selectedGroup) {
            srOnlyHeading += " grouped by term"
        } else if ("baseRoleLabel" === props.selectedGroup) {
            srOnlyHeading += " grouped by role"
        }

        return (
            <React.Fragment>
            <h2 className="sr-only">{srOnlyHeading}</h2>
            <table className="rvt-m-bottom-md">
                <caption className="sr-only">Table of courses</caption>
                <thead className="tableHeadOverride">
                <tr>
                    <th scope="col" id="courseName">
                        <LinkHeader anchorValue="Course Name" newSortValue="courseName" orderKey={props.orderKey} onClick={props.handleOrdering} />
                    </th>
                    <th scope="col" id="fav">Favorite</th>
                    <th scope="col" id="vis">Visibility</th>
                    <th scope="col" id="courseCode">
                        <LinkHeader anchorValue="Course Code/SIS ID" newSortValue="courseCode" orderKey={props.orderKey} onClick={props.handleOrdering} />
                    </th>
                    <th scope="col" id="nickname">Nickname</th>
                    <th scope="col" id="role">Role</th>
                    <th scope="col" id="term">
                        <LinkHeader anchorValue="Term" newSortValue="term.name" orderKey={props.orderKey} onClick={props.handleOrdering} />
                    </th>
                    <th scope="col" id="pub">Published</th>
                </tr>
                </thead>
                <DataGrouping data={props.groupedCourses} updateCourseInState={props.updateCourseInState} />
            </table>
            </React.Fragment>
        );
    }
}

function EnrollmentOptions(props) {
    var availableEnrollments = _.chain(props.courses).map("enrollmentClassification").uniqBy("name").orderBy("order", "asc").value();
    const enrollmentOptions = availableEnrollments.map((enrollmentClassification) => (
            <li key={enrollmentClassification.name}>
                <input type="checkbox" id={enrollmentClassification.name} name="enrollmentCheckboxes" className="filter-input"
                    value={enrollmentClassification.name} onChange={props.handleFilter}
                    checked={props.filteredEnrollments.includes(enrollmentClassification.name)} />
                <label htmlFor={enrollmentClassification.name} className="rvt-m-right-sm rvt-text-nobr">{enrollmentClassification.text}</label>
            </li>
        ))


    return (
            <ul className="rvt-plain-list">
                {enrollmentOptions}
            </ul>
    );
}

function groupAndSortBuckets(courses, groupKey, groupSort, orderKey) {
    var dataMap = _.chain(courses).orderBy([item => _.get(item, orderKey.key).toLowerCase()], orderKey.direction).groupBy(groupKey).value();

    var sortedMap = new Map();
    var sortData = _.chain(courses)
        .orderBy(groupSort.key, groupSort.direction)
        .map(groupKey)
        .uniq()
        .value();

    for (var index in sortData) {
        var key = sortData[index];
        sortedMap.set(key, dataMap[key]);
    }

    return sortedMap;
}

function LinkHeader(props) {
    if (props.orderKey.key==props.newSortValue) {
        if (props.orderKey.direction=='asc') {
            return (
                <React.Fragment>
                    <a href="#" className="rvt-link-bold iconPointer" onClick={props.onClick} title={`Click to sort by ${props.anchorValue}, descending`}
                        new-sort-value={props.newSortValue}>{props.anchorValue}</a>
                    <RvtSvg icon="rvt-icon-arrow-up" />
                </React.Fragment>
            );
        } else {
            return (
                <React.Fragment>
                    <a href="#" className="rvt-link-bold iconPointer" onClick={props.onClick} title={`Click to sort by ${props.anchorValue}, ascending`}
                        new-sort-value={props.newSortValue}>{props.anchorValue}</a>
                    <RvtSvg icon="rvt-icon-arrow-down" />
                </React.Fragment>
            );
        }
    } else {
        return (
            <React.Fragment>
                <a href="#" className="rvt-link-bold iconPointer" onClick={props.onClick} title={`Click to sort by ${props.anchorValue}, ascending`}
                    new-sort-value={props.newSortValue}>{props.anchorValue}</a>
            </React.Fragment>
        );
    }
}

export default App
