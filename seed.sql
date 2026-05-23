insert into categories (name_ar, name_en, description, icon, status) values
('المواد الغذائية الجافة','Dry Food','مكرونة، أرز، بقوليات، دقيق، سكر ومنتجات أساسية.','🌾','active'),
('الزيوت والسمن','Oils and Ghee','زيوت طعام، سمن نباتي وحيواني وعبوات جملة.','🛢️','active'),
('الألبان ومنتجاتها','Dairy','أجبان، لبن، زبادي، قشطة ومنتجات مبردة.','🧀','active'),
('المعلبات','Canned Food','تونة، فول، صلصة وخضروات معلبة.','🥫','active')
on conflict do nothing;
