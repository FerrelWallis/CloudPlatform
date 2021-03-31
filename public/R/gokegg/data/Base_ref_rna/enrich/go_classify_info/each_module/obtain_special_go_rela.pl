#!usr/bin/perl -w

use strict;

die "usage:perl $0 <gene_ontology.1_2.obo> <GO ID>\n" unless @ARGV == 2;

open A,$ARGV[0];

my %h1;my %tar;my %h2;my %func;my %rev_rela;

my %tr=(
    "is" => '1',
    "part_of" => '2',
    "regulates" => '3',
    "negatively_regulates" => '4',
    "positively_regulates" => '5'
);
$/="Term]\nid: ";
<A>;
while(<A>){
    chomp;my @a=split /\n+/;
    my @b=split /: /,$a[1]; #function
    my @c=split /: /,$a[2]; #main function
    #print "$a[0]\t$b[1]\t$c[1]\n";
    $func{$a[0]}=$c[1];
    foreach my $k1(3..$#a){
        my @dd=split /\s+/,$a[$k1];
        if($a[$k1]=~/is_a:/){
            #print "$a[0]\t$dd[1]\n";
            $h1{$a[0]}{$dd[1]}="is";
            $rev_rela{$dd[1]}{$a[0]}="is";
        }
        if($a[$k1]=~/^relationship:/){
            #$h1{$a[0]}{$dd[2]}=$dd[1];
        }
    }
}
close A;
$/="\n";
#print parent relation
my %temp=();$temp{$ARGV[1]}=1;
while(1){
    my %part=%temp;%temp=();
    foreach my $k1(sort keys %part){
        if(exists $h1{$k1}){
            foreach my $k2(sort keys %{$h1{$k1}}){
                print "$k1\t$k2\tparent\n";
                $temp{$k2}=1;
            }
        }
    }
    last if(0 == scalar (keys %temp));
}
#print son relation
%temp=();$temp{$ARGV[1]}=1;
while(1){
    my %part=%temp;%temp=();
    foreach my $k1(sort keys %part){
        if(exists $rev_rela{$k1}){
            foreach my $k2(sort keys %{$rev_rela{$k1}}){
                print "$k2\t$k1\tson\n";
                $temp{$k2}=1;
            }
        }
    }
    last;
    #last if(0 == scalar (keys %temp));
}
=c
foreach my $k1(sort keys %rela){
    foreach my $k2(sort keys %{$rela{$k1}}){
        foreach my $k3(sort keys %{$rela{$k1}{$k2}}){
            print "$k1\t$k2\t$tr{$k3}\n";
        }
    }
}
