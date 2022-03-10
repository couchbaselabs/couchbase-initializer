import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, from } from 'rxjs';
import { map, mergeMap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class TemplateService {

  private loadedManifest: any;

  constructor(private http: HttpClient) {

  }

  loadManifest(): Observable<any> {
    let options = {
      headers: new HttpHeaders({
        'Access-Control-Allow-Origin': '*'
      })
    };
    return this.http.get("http://localhost:8080/manifest.json", options).pipe(map(v => {
      this.loadedManifest = v;
      return v
    }))
  }

  getLanguageGroups(): Observable<LanguageTemplateGroup> {
    return this.loadManifest().pipe(mergeMap(v => {
      let groups: LanguageTemplateGroup[] = [];
      for (let langGroup of v["children"]) {
        let groupKey = langGroup["path"];
        let groupName = langGroup["name"];

        let languages: LanguageTemplate[] = [];
        for (let lang of langGroup["children"]) {
          languages.push(new LanguageTemplate(lang["path"], lang["name"]));
        }
        groups.push(new LanguageTemplateGroup(groupKey, groupName, languages));
      }
      return groups
    }));
  }

  getProjectGroups(value: any): Observable<ProjectTemplateGroup> {
    let langKey = value["entry"].split(".")[1];

    return this.loadManifest().pipe(mergeMap(v => {
      let groups: ProjectTemplateGroup[] = [];
      for (let langGroup of v["children"]) {
        for (let lang of langGroup["children"]) {
          if (lang["path"] == langKey) {
            if (lang["childSelectionLabel"] == "Project") {
              let projects: ProjectTemplate[] = [];
              for (let proj of lang["children"]) {
                projects.push(new ProjectTemplate(proj["path"], proj["name"]));
              }
              groups.push(new ProjectTemplateGroup("", "", projects));
            } else {
              for (let projectGroup of lang["children"]) {
                let projects: ProjectTemplate[] = [];
                for (let proj of projectGroup["children"]) {
                  projects.push(new ProjectTemplate(proj["path"], proj["name"]));
                }
                groups.push(new ProjectTemplateGroup(projectGroup["path"], projectGroup["name"], projects));
              }
            }
          }
        }
      }
      return groups
    }));
  }

  getMetadataGroups(path: string): Observable<MetadataTemplateGroup> {
    let options = {
      headers: new HttpHeaders({
        'Access-Control-Allow-Origin': '*'
      })
    };
    return this.http.get<any>("http://localhost:8080/project/" + path, options).pipe(mergeMap(v => {
      let groups: MetadataTemplateGroup[] = [];
      for (let section of v["sections"]) {
        let groupKey = section["key"];
        let groupName = section["name"];
        let metadata: MetadataTemplateField[] = [];
        for (let p of section["parameters"]) {
          let options: MetadataTemplateOption[] = [];
          if (p["options"]) {
            for (let o of p["options"]) {
              options.push(new MetadataTemplateOption(o["key"], o["name"]));
            }
          }
          metadata.push(new MetadataTemplateField(p["key"], p["name"], options, p["type"], p["defaultValue"]));
        }
        groups.push(new MetadataTemplateGroup(groupKey, groupName, metadata));
      }
      return groups;
    }))
  }

  runDownload(path: string, params: any): Observable<Blob> {
    let options = {
      headers: new HttpHeaders({
        'Access-Control-Allow-Origin': '*'
      }),
      responseType: 'blob' as 'json',
      params: params
    };

    return this.http.post<Blob>("http://localhost:8080/download/" + path, null, options);
  }
}


export class LanguageTemplateGroup {
  groupKey: string;
  groupName: string;
  languages: LanguageTemplate[];

  constructor(groupKey: string, groupName: string, languages: LanguageTemplate[]) {
    this.groupKey = groupKey;
    this.groupName = groupName;
    this.languages = languages;
  }
}

export class LanguageTemplate {
  key: string;
  value: string;

  constructor(key: string, value: string) {
    this.key = key;
    this.value = value;
  }
}

export class ProjectTemplateGroup {
  groupKey: string;
  groupName: string;
  projects: ProjectTemplate[];

  constructor(groupKey: string, groupName: string, projects: ProjectTemplate[]) {
    this.groupKey = groupKey;
    this.groupName = groupName;
    this.projects = projects;
  }
}

export class ProjectTemplate {
  key: string;
  value: string;

  constructor(key: string, value: string) {
    this.key = key;
    this.value = value;
  }
}

export class MetadataTemplateGroup {
  groupKey: string;
  groupName: string;
  fields: MetadataTemplateField[];

  constructor(groupKey: string, groupName: string, fields: MetadataTemplateField[]) {
    this.groupKey = groupKey;
    this.groupName = groupName;
    this.fields = fields;
  }
}

export class MetadataTemplateField {
  key: string;
  name: string;
  type?: string;
  defaultValue?: string;
  options: MetadataTemplateOption[];
  constructor(key: string, name: string, options: MetadataTemplateOption[], type?: string, defaultValue?: string) {
    this.key = key;
    this.name = name;
    this.type = type;
    this.defaultValue = defaultValue;
    this.options = options;
  }
}

export class MetadataTemplateOption {
  key: string;
  name: string;

  constructor(key: string, name: string) {
    this.key = key;
    this.name = name;
  }
}