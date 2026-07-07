# Tabs

Organizes content into multiple sections, displaying only one section at a time while keeping others easily accessible through a tabbed navigation interface. Tabs help structure information without overwhelming the user.

Part of the Harmonia Alpine.js component library. Every directive uses the `x-h-` prefix.

## Usage

Use tabs to group related content or functionality, allowing users to switch between sections without leaving the current view.

## Directives

`x-h-tabs` is the root. The directives compose one component and must be nested as shown in the Examples below (the library throws at runtime when a required ancestor is missing):

- `x-h-tabs`
- `x-h-tab-bar`
- `x-h-tab-list`
- `x-h-tab`
- `x-h-tab-action`
- `x-h-tab-list-actions`
- `x-h-tab-list-action`
- `x-h-tabs-content`

## API

### Attributes

#### x-h-tabs

| Attribute        | Type                         | Required | Description                             |
| ---------------- | ---------------------------- | -------- | --------------------------------------- |
| data-orientation | `horizontal`<br />`vertical` | true     | Changes the orientation of the tab list |

#### x-h-tab-bar

| Attribute     | Type                          | Required | Description                                                                                 |
| ------------- | ----------------------------- | -------- | ------------------------------------------------------------------------------------------- |
| data-floating | boolean                       | false    | Floating style tab list.                                                                    |
| data-size     | `default`<br />`sm`<br />`lg` | false    | Height of the tab bar. Ignored when the tab bar is floating or the orientation is vertical. |

#### x-h-tabs-content

| Attribute | Type    | Required | Description               |
| --------- | ------- | -------- | ------------------------- |
| hidden    | boolean | false    | Show/hide the tab content |

#### x-h-tab-list-action

| Attribute    | Type                         | Required | Description                                            |
| ------------ | ---------------------------- | -------- | ------------------------------------------------------ |
| data-variant | `outline`<br />`transparent` | false    | Changes the style of the button. Default is `outline`. |

### Modifiers

#### x-h-tab-list-actions

| Modifier | Description                                                     |
| -------- | --------------------------------------------------------------- |
| end      | Tab action will be placed at the end of the tab list container. |

## Examples

### Sizes

```html
<div x-h-tabs data-orientation="horizontal">
  <div x-h-tab-bar data-size="sm">
    <div x-h-tab-list>
      <button id="smt1" x-h-tab aria-controls="smt1c" aria-selected="true">Tab 1</button>
      <button id="smt2" x-h-tab aria-controls="smt2c">Tab 2</button>
    </div>
  </div>
  <div x-h-tabs-content id="smt1c" aria-labelledby="smt1" hidden></div>
  <div x-h-tabs-content id="smt2c" aria-labelledby="smt2" hidden></div>
</div>
<div x-h-tabs data-orientation="horizontal">
  <div x-h-tab-bar>
    <div x-h-tab-list>
      <button id="dt1" x-h-tab aria-controls="dt1c" aria-selected="true">Tab 1</button>
      <button id="dt2" x-h-tab aria-controls="dt2c">Tab 2</button>
    </div>
  </div>
  <div x-h-tabs-content id="dt1c" aria-labelledby="dt1" hidden></div>
  <div x-h-tabs-content id="dt2c" aria-labelledby="dt2" hidden></div>
</div>
<div x-h-tabs data-orientation="horizontal">
  <div x-h-tab-bar data-size="lg">
    <div x-h-tab-list>
      <button id="lgt1" x-h-tab aria-controls="lgt1c" aria-selected="true">Tab 1</button>
      <button id="lgt2" x-h-tab aria-controls="lgt2c">Tab 2</button>
    </div>
  </div>
  <div x-h-tabs-content id="lgt1c" aria-labelledby="lgt1" hidden></div>
  <div x-h-tabs-content id="lgt2c" aria-labelledby="lgt2" hidden></div>
</div>
```

### Scrollable tab content

```html
<div x-h-tabs data-orientation="horizontal" style="height:10rem">
  <div x-h-tab-bar>
    <div x-h-tab-list>
      <button id="stce" x-h-tab aria-controls="stcec" aria-selected="true">Tab 1</button>
    </div>
  </div>
  <div class="relative" x-h-tabs-content id="stcec" aria-labelledby="stce">
    <div class="position-fit absolute overflow-auto">
      <img src="/harmonia/logo/harmonia.svg" alt="@harmonia" width="240px" />
    </div>
  </div>
</div>
```

### Horizontal tabs

```html
<div x-h-tabs data-orientation="horizontal" x-data="{ activeTabId: 'hit1' }">
  <div x-h-tab-bar>
    <div x-h-tab-list>
      <button x-h-tab id="hit1" aria-controls="hit1c" :aria-selected="activeTabId === 'hit1'" @click="activeTabId = 'hit1'">Tab 1</button>
      <button x-h-tab id="hit2" aria-controls="hit2c" :aria-selected="activeTabId === 'hit2'" @click="activeTabId = 'hit2'">Tab 2</button>
      <button x-h-tab id="hit3" aria-controls="hit3c" :aria-selected="activeTabId === 'hit3'" @click="activeTabId = 'hit3'">Tab 3</button>
    </div>
  </div>
  <div x-h-tabs-content id="hit1c" aria-labelledby="hit1" :hidden="activeTabId !== 'hit1'">
    <div class="p-2">Tab 1 Content</div>
  </div>
  <div x-h-tabs-content id="hit2c" aria-labelledby="hit2" :hidden="activeTabId !== 'hit2'">
    <div class="p-2">Tab 2 Content</div>
  </div>
  <div x-h-tabs-content id="hit3c" aria-labelledby="hit3" :hidden="activeTabId !== 'hit3'">
    <div class="p-2">Tab 3 Content</div>
  </div>
</div>
```

### Horizontal tabs with icon and close button

```html
<div x-h-tabs data-orientation="horizontal" x-data="{ activeTabId: 'hib1' }">
  <div x-h-tab-bar>
    <div x-h-tab-list>
      <button x-h-tab id="hib1" aria-controls="hib1c" :aria-selected="activeTabId === 'hib1'" @click="activeTabId = 'hib1'">
        <i x-h-lucide role="img" data-lucide="file"></i>
        Tab 1
        <span x-h-tab-action>
          <i x-h-lucide role="img" data-lucide="x"></i>
        </span>
      </button>
      <button x-h-tab id="hib2" aria-controls="hib2c" :aria-selected="activeTabId === 'hib2'" @click="activeTabId = 'hib2'">
        <i x-h-lucide role="img" data-lucide="file"></i>
        Tab 2
        <span x-h-tab-action>
          <i x-h-lucide role="img" data-lucide="x"></i>
        </span>
      </button>
      <button x-h-tab id="hib3" aria-controls="hib3c" :aria-selected="activeTabId === 'hib3'" @click="activeTabId = 'hib3'">
        <i x-h-lucide role="img" data-lucide="file"></i>
        Tab 3
        <span x-h-tab-action>
          <i x-h-lucide role="img" data-lucide="x"></i>
        </span>
      </button>
    </div>
  </div>
  <div x-h-tabs-content id="hib1c" aria-labelledby="hib1" :hidden="activeTabId !== 'hib1'">
    <div class="p-2">Tab 1 Content</div>
  </div>
  <div x-h-tabs-content id="hib2c" aria-labelledby="hib2" :hidden="activeTabId !== 'hib2'">
    <div class="p-2">Tab 2 Content</div>
  </div>
  <div x-h-tabs-content id="hib3c" aria-labelledby="hib3" :hidden="activeTabId !== 'hib3'">
    <div class="p-2">Tab 3 Content</div>
  </div>
</div>
```

### Horizontal tabs with actions

```html
<div x-h-tabs data-orientation="horizontal">
  <div x-h-tab-bar>
    <div x-h-tab-list>
      <button x-h-tab id="hitwa1" aria-controls="hitwa1c" aria-selected="true">
        Tab 1
        <span x-h-tab-action>
          <i x-h-lucide role="img" data-lucide="x"></i>
        </span>
      </button>
    </div>
    <div x-h-tab-list-actions>
      <button x-h-tab-list-action data-variant="transparent" aria-label="add tab button">
        <i x-h-lucide role="img" data-lucide="plus"></i>
      </button>
    </div>
  </div>
  <div x-h-tabs-content id="hitwa1c" aria-labelledby="hitwa1">
    <div class="p-2">Tab 1 Content</div>
  </div>
</div>
```

### Horizontal tabs with actions (end)

```html
<div x-h-tabs data-orientation="horizontal">
  <div x-h-tab-bar>
    <div x-h-tab-list>
      <button x-h-tab id="hitwae1" aria-controls="hitwae1c" aria-selected="true">
        Tab 1
        <span x-h-tab-action>
          <i x-h-lucide role="img" data-lucide="x"></i>
        </span>
      </button>
    </div>
    <div x-h-tab-list-actions.end>
      <button x-h-tab-list-action aria-label="menu button">
        <i x-h-lucide role="img" data-lucide="ellipsis"></i>
      </button>
    </div>
  </div>
  <div x-h-tabs-content id="hitwae1c" aria-labelledby="hitwae1">
    <div class="p-2">Tab 1 Content</div>
  </div>
</div>
```

### Horizontal float tabs

```html
<div x-h-tabs data-orientation="horizontal" x-data="{ activeTabId: 'ht1' }">
  <div x-h-tab-bar data-floating="true">
    <div x-h-tab-list>
      <button x-h-tab id="ht1" aria-controls="ht1c" :aria-selected="activeTabId === 'ht1'" @click="activeTabId = 'ht1'">Tab 1</button>
      <button x-h-tab id="ht2" aria-controls="ht2c" :aria-selected="activeTabId === 'ht2'" @click="activeTabId = 'ht2'">Tab 2</button>
      <button x-h-tab id="ht3" aria-controls="ht3c" :aria-selected="activeTabId === 'ht3'" @click="activeTabId = 'ht3'">Tab 3</button>
    </div>
  </div>
  <div x-h-tabs-content id="ht1c" aria-labelledby="ht1" :hidden="activeTabId !== 'ht1'">
    <div class="p-2">Tab 1 Content</div>
  </div>
  <div x-h-tabs-content id="ht2c" aria-labelledby="ht2" :hidden="activeTabId !== 'ht2'">
    <div class="p-2">Tab 2 Content</div>
  </div>
  <div x-h-tabs-content id="ht3c" aria-labelledby="ht3" :hidden="activeTabId !== 'ht3'">
    <div class="p-2">Tab 3 Content</div>
  </div>
</div>
```

### Horizontal float tabs that fit to size

You can make the tab bar fit to the size of the tab list by adding the `w-max` class.

```html
<div x-h-tabs data-orientation="horizontal">
  <div x-h-tab-bar data-floating="true" class="w-max">
    <div x-h-tab-list>
      <button x-h-tab id="htfts1" aria-controls="htfts1c" aria-selected="true">Sign In</button>
      <button x-h-tab id="htfts2" aria-controls="htfts2c">Sign Up</button>
    </div>
  </div>
  <div x-h-tabs-content id="htfts1c" aria-labelledby="htfts1">
    <div class="p-2">Sign In</div>
  </div>
  <div x-h-tabs-content id="htfts2c" aria-labelledby="htfts2" hidden="true">
    <div class="p-2">Sign Up</div>
  </div>
</div>
```

### Horizontal float tabs with icon and close button

```html
<div x-h-tabs data-orientation="horizontal" x-data="{ activeTabId: 'hbt1' }">
  <div x-h-tab-bar data-floating="true">
    <div x-h-tab-list>
      <button x-h-tab id="hbt1" aria-controls="hbt1c" :aria-selected="activeTabId === 'hbt1'" @click="activeTabId = 'hbt1'">
        <i x-h-lucide role="img" data-lucide="file"></i>
        Tab 1
        <span x-h-tab-action>
          <i x-h-lucide role="img" data-lucide="x"></i>
        </span>
      </button>
      <button x-h-tab id="hbt2" aria-controls="hbt2c" :aria-selected="activeTabId === 'hbt2'" @click="activeTabId = 'hbt2'">
        <i x-h-lucide role="img" data-lucide="file"></i>
        Tab 2
        <span x-h-tab-action>
          <i x-h-lucide role="img" data-lucide="x"></i>
        </span>
      </button>
      <button x-h-tab id="hbt3" aria-controls="hbt3c" :aria-selected="activeTabId === 'hbt3'" @click="activeTabId = 'hbt3'">
        <i x-h-lucide role="img" data-lucide="file"></i>
        Tab 3
        <span x-h-tab-action>
          <i x-h-lucide role="img" data-lucide="x"></i>
        </span>
      </button>
    </div>
  </div>
  <div x-h-tabs-content id="hbt1c" aria-labelledby="hbt1" :hidden="activeTabId !== 'hbt1'">
    <div class="p-2">Tab 1 Content</div>
  </div>
  <div x-h-tabs-content id="hbt2c" aria-labelledby="hbt2" :hidden="activeTabId !== 'hbt2'">
    <div class="p-2">Tab 2 Content</div>
  </div>
  <div x-h-tabs-content id="hbt3c" aria-labelledby="hbt3" :hidden="activeTabId !== 'hbt3'">
    <div class="p-2">Tab 3 Content</div>
  </div>
</div>
```

### Horizontal float tabs with actions

```html
<div x-h-tabs data-orientation="horizontal">
  <div x-h-tab-bar data-floating="true">
    <div x-h-tab-list>
      <button x-h-tab id="hftwa1" aria-controls="hftwa1c" aria-selected="true">
        Tab 1
        <span x-h-tab-action>
          <i x-h-lucide role="img" data-lucide="x"></i>
        </span>
      </button>
    </div>
    <div x-h-tab-list-actions>
      <button x-h-tab-list-action data-variant="transparent" aria-label="add tab button">
        <i x-h-lucide role="img" data-lucide="plus"></i>
      </button>
    </div>
  </div>
  <div x-h-tabs-content id="hftwa1c" aria-labelledby="hftwa1">
    <div class="p-2">Tab 1 Content</div>
  </div>
</div>
```

### Horizontal float tabs with actions (end)

```html
<div x-h-tabs data-orientation="horizontal">
  <div x-h-tab-bar data-floating="true">
    <div x-h-tab-list>
      <button x-h-tab id="hftwae1" aria-controls="hftwae1c" aria-selected="true">
        Tab 1
        <span x-h-tab-action>
          <i x-h-lucide role="img" data-lucide="x"></i>
        </span>
      </button>
    </div>
    <div x-h-tab-list-actions.end>
      <button x-h-tab-list-action aria-label="menu button">
        <i x-h-lucide role="img" data-lucide="ellipsis"></i>
      </button>
    </div>
  </div>
  <div x-h-tabs-content id="hftwae1c" aria-labelledby="hftwae1">
    <div class="p-2">Tab 1 Content</div>
  </div>
</div>
```

### Vertical tabs

```html
<div x-h-tabs data-orientation="vertical" x-data="{ activeTabId: 'vit1' }">
  <div x-h-tab-bar>
    <div x-h-tab-list>
      <button x-h-tab id="vit1" aria-controls="vit1c" :aria-selected="activeTabId === 'vit1'" @click="activeTabId = 'vit1'">Tab 1</button>
      <button x-h-tab id="vit2" aria-controls="vit2c" :aria-selected="activeTabId === 'vit2'" @click="activeTabId = 'vit2'">Tab 2</button>
      <button x-h-tab id="vit3" aria-controls="vit3c" :aria-selected="activeTabId === 'vit3'" @click="activeTabId = 'vit3'">Tab 3</button>
    </div>
  </div>
  <div x-h-tabs-content id="vit1c" aria-labelledby="vit1" :hidden="activeTabId !== 'vit1'">
    <div class="p-2">Tab 1 Content</div>
  </div>
  <div x-h-tabs-content id="vit2c" aria-labelledby="vit2" :hidden="activeTabId !== 'vit2'">
    <div class="p-2">Tab 2 Content</div>
  </div>
  <div x-h-tabs-content id="vit3c" aria-labelledby="vit3" :hidden="activeTabId !== 'vit3'">
    <div class="p-2">Tab 3 Content</div>
  </div>
</div>
```

### Vertical tabs with icon and close button

```html
<div x-h-tabs data-orientation="vertical" x-data="{ activeTabId: 'vib1' }">
  <div x-h-tab-bar>
    <div x-h-tab-list>
      <button x-h-tab id="vib1" aria-controls="vib1c" :aria-selected="activeTabId === 'vib1'" @click="activeTabId = 'vib1'">
        <i x-h-lucide role="img" data-lucide="file"></i>
        Tab 1
        <span x-h-tab-action>
          <i x-h-lucide role="img" data-lucide="x"></i>
        </span>
      </button>
      <button x-h-tab id="vib2" aria-controls="vib2c" :aria-selected="activeTabId === 'vib2'" @click="activeTabId = 'vib2'">
        <i x-h-lucide role="img" data-lucide="file"></i>
        Tab 2
        <span x-h-tab-action>
          <i x-h-lucide role="img" data-lucide="x"></i>
        </span>
      </button>
      <button x-h-tab id="vib3" aria-controls="vib3c" :aria-selected="activeTabId === 'vib3'" @click="activeTabId = 'vib3'">
        <i x-h-lucide role="img" data-lucide="file"></i>
        Tab 3
        <span x-h-tab-action>
          <i x-h-lucide role="img" data-lucide="x"></i>
        </span>
      </button>
    </div>
  </div>
  <div x-h-tabs-content id="vib1c" aria-labelledby="vib1" :hidden="activeTabId !== 'vib1'">
    <div class="p-2">Tab 1 Content</div>
  </div>
  <div x-h-tabs-content id="vib2c" aria-labelledby="vib2" :hidden="activeTabId !== 'vib2'">
    <div class="p-2">Tab 2 Content</div>
  </div>
  <div x-h-tabs-content id="vib3c" aria-labelledby="vib3" :hidden="activeTabId !== 'vib3'">
    <div class="p-2">Tab 3 Content</div>
  </div>
</div>
```

### Vertical tabs with actions

```html
<div x-h-tabs data-orientation="vertical" style="height:8rem">
  <div x-h-tab-bar>
    <div x-h-tab-list>
      <button x-h-tab id="vitwa1" aria-controls="vitwa1c" aria-selected="true">
        Tab 1
        <span x-h-tab-action>
          <i x-h-lucide role="img" data-lucide="x"></i>
        </span>
      </button>
    </div>
    <div x-h-tab-list-actions>
      <button x-h-tab-list-action data-variant="transparent" aria-label="add tab button">
        <i x-h-lucide role="img" data-lucide="plus"></i>
      </button>
    </div>
  </div>
  <div x-h-tabs-content id="vitwa1c" aria-labelledby="vitwa1">
    <div class="p-2">Tab 1 Content</div>
  </div>
</div>
```

### Vertical tabs with actions (end)

```html
<div x-h-tabs data-orientation="vertical" style="height:8rem">
  <div x-h-tab-bar>
    <div x-h-tab-list>
      <button x-h-tab id="vitwae1" aria-controls="vitwae1c" aria-selected="true">
        Tab 1
        <span x-h-tab-action>
          <i x-h-lucide role="img" data-lucide="x"></i>
        </span>
      </button>
    </div>
    <div x-h-tab-list-actions.end>
      <button x-h-tab-list-action aria-label="menu button">
        <i x-h-lucide role="img" data-lucide="ellipsis"></i>
      </button>
    </div>
  </div>
  <div x-h-tabs-content id="vitwae1c" aria-labelledby="vitwae1">
    <div class="p-2">Tab 1 Content</div>
  </div>
</div>
```

### Vertical float tabs

```html
<div x-h-tabs data-orientation="vertical" x-data="{ activeTabId: 'vt1' }">
  <div x-h-tab-bar data-floating="true">
    <div x-h-tab-list>
      <button x-h-tab id="vt1" aria-controls="vt1c" :aria-selected="activeTabId === 'vt1'" @click="activeTabId = 'vt1'">Tab 1</button>
      <button x-h-tab id="vt2" aria-controls="vt2c" :aria-selected="activeTabId === 'vt2'" @click="activeTabId = 'vt2'">Tab 2</button>
      <button x-h-tab id="vt3" aria-controls="vt3c" :aria-selected="activeTabId === 'vt3'" @click="activeTabId = 'vt3'">Tab 3</button>
    </div>
  </div>
  <div x-h-tabs-content id="vt1c" aria-labelledby="vt1" :hidden="activeTabId !== 'vt1'">
    <div class="p-2">Tab 1 Content</div>
  </div>
  <div x-h-tabs-content id="vt2c" aria-labelledby="vt2" :hidden="activeTabId !== 'vt2'">
    <div class="p-2">Tab 2 Content</div>
  </div>
  <div x-h-tabs-content id="vt3c" aria-labelledby="vt3" :hidden="activeTabId !== 'vt3'">
    <div class="p-2">Tab 3 Content</div>
  </div>
</div>
```

### Vertical float tabs with icon and close button

```html
<div x-h-tabs data-orientation="vertical" x-data="{ activeTabId: 'vbt1' }">
  <div x-h-tab-bar data-floating="true">
    <div x-h-tab-list>
      <button x-h-tab id="vbt1" aria-controls="vbt1c" :aria-selected="activeTabId === 'vbt1'" @click="activeTabId = 'vbt1'">
        <i x-h-lucide role="img" data-lucide="file"></i>
        Tab 1
        <span x-h-tab-action>
          <i x-h-lucide role="img" data-lucide="x"></i>
        </span>
      </button>
      <button x-h-tab id="vbt2" aria-controls="vbt2c" :aria-selected="activeTabId === 'vbt2'" @click="activeTabId = 'vbt2'">
        <i x-h-lucide role="img" data-lucide="file"></i>
        Tab 2
        <span x-h-tab-action>
          <i x-h-lucide role="img" data-lucide="x"></i>
        </span>
      </button>
      <button x-h-tab id="vbt3" aria-controls="vbt3c" :aria-selected="activeTabId === 'vbt3'" @click="activeTabId = 'vbt3'">
        <i x-h-lucide role="img" data-lucide="file"></i>
        Tab 3
        <span x-h-tab-action>
          <i x-h-lucide role="img" data-lucide="x"></i>
        </span>
      </button>
    </div>
  </div>
  <div x-h-tabs-content id="vbt1c" aria-labelledby="vbt1" :hidden="activeTabId !== 'vbt1'">
    <div class="p-2">Tab 1 Content</div>
  </div>
  <div x-h-tabs-content id="vbt2c" aria-labelledby="vbt2" :hidden="activeTabId !== 'vbt2'">
    <div class="p-2">Tab 2 Content</div>
  </div>
  <div x-h-tabs-content id="vbt3c" aria-labelledby="vbt3" :hidden="activeTabId !== 'vbt3'">
    <div class="p-2">Tab 3 Content</div>
  </div>
</div>
```

### Vertical float tabs with actions

```html
<div x-h-tabs data-orientation="vertical" style="height:8rem">
  <div x-h-tab-bar data-floating="true">
    <div x-h-tab-list>
      <button x-h-tab id="vftwa1" aria-controls="vftwa1c" aria-selected="true">
        Tab 1
        <span x-h-tab-action>
          <i x-h-lucide role="img" data-lucide="x"></i>
        </span>
      </button>
    </div>
    <div x-h-tab-list-actions>
      <button x-h-tab-list-action data-variant="transparent" aria-label="add tab button">
        <i x-h-lucide role="img" data-lucide="plus"></i>
      </button>
    </div>
  </div>
  <div x-h-tabs-content id="vftwa1c" aria-labelledby="vftwa1">
    <div class="p-2">Tab 1 Content</div>
  </div>
</div>
```

### Vertical float tabs with actions (end)

```html
<div x-h-tabs data-orientation="vertical" style="height:8rem">
  <div x-h-tab-bar data-floating="true">
    <div x-h-tab-list>
      <button x-h-tab id="vftwae1" aria-controls="vftwae1c" aria-selected="true">
        Tab 1
        <span x-h-tab-action>
          <i x-h-lucide role="img" data-lucide="x"></i>
        </span>
      </button>
    </div>
    <div x-h-tab-list-actions.end>
      <button x-h-tab-list-action data-variant="outline" aria-label="menu button">
        <i x-h-lucide role="img" data-lucide="ellipsis"></i>
      </button>
    </div>
  </div>
  <div x-h-tabs-content id="vftwae1c" aria-labelledby="vftwae1">
    <div class="p-2">Tab 1 Content</div>
  </div>
</div>
```

Full docs: https://www.codbex.com/harmonia/components/tabs.html

## Notes

- Directive values are Alpine expressions, so quote string literals: `x-h-...="'Label'"`.
- Components render only after Alpine has registered Harmonia. See SKILL.md for setup.
