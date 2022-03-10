import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { ReactiveFormsModule } from '@angular/forms';
import { AppRoutingModule } from './app-routing.module';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AppComponent } from './app.component';
import { TemplateComponent } from './template/template.component';
import { MatSelectModule } from '@angular/material/select';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { HttpClientModule } from '@angular/common/http';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatCardModule } from '@angular/material/card';
import { MatRadioModule } from '@angular/material/radio';
@NgModule({
  declarations: [
    AppComponent,
    TemplateComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    ReactiveFormsModule,
    HttpClientModule,
    MatSelectModule,
    MatToolbarModule,
    MatButtonModule,
    MatFormFieldModule,
    MatGridListModule,
    MatInputModule,
    MatCardModule,
    MatRadioModule
  ],
  providers: [
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
